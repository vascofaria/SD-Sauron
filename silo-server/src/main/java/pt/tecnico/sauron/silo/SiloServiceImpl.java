package pt.tecnico.sauron.silo;

import pt.tecnico.sauron.silo.exceptions.SiloException;

import pt.tecnico.sauron.silo.eyes.Coords;

import pt.tecnico.sauron.silo.grpc.*;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import static io.grpc.Status.INVALID_ARGUMENT;
import static io.grpc.Status.NOT_FOUND;

import java.util.List;
import java.util.ArrayList;
import java.util.TimerTask;

import com.google.protobuf.Timestamp;

import java.util.Random;
import java.util.Timer;

import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import static io.grpc.Status.ALREADY_EXISTS;

public class SiloServiceImpl extends SiloGrpc.SiloImplBase {

	private static final Integer  N_REPLICS = 10; 
	private static final Integer  MAX_ATTEMPTS = 3;

	private List<Integer>         replicTimestamp;
	private Integer               replicNumber;
	private List<UpdateLog>       updateLogs;
	private List<List<Integer>>   timestampTable;
	private List<Observation>     executedReportRequests;
	private Silo                  silo;
	private Timer                 timer;

	public SiloServiceImpl(Integer replicNumber, String zooHost, String zooPort, String path, Integer timeToSend) {
		super();
		this.silo            = new Silo();
		this.replicTimestamp = new ArrayList<>();
		this.replicNumber    = replicNumber;
		this.updateLogs      = new ArrayList<>();
		this.timestampTable  = new ArrayList<>();
		this.executedReportRequests = new ArrayList<>();

		// create timer object
        // set it as a daemon so the JVM doesn't wait for it when quitting
		this.timer = new Timer(/*isDaemon*/ true);

		// create timer task object
       	SendGossipMsgTask timerTask = new SendGossipMsgTask(zooHost, zooPort, path);

       	// Set timer to execute the task every 30 seconds
    	timer.schedule(timerTask, /*delay*/ 0 * 1000, /*period*/ timeToSend * 1000);

    	// init replicTimestamp and the timestamptable with '0's
		for(int i = 0; i < N_REPLICS; i++) {
			this.replicTimestamp.add(0);
			this.timestampTable.add(new ArrayList<>());
			for (int j = 0; j < N_REPLICS; j++)
				this.timestampTable.set(i, new ArrayList<>(N_REPLICS));
		}
	}

	/*
		This inner class (SendGossipMsgTask) is responsible for sending the Gossip msg
		with the update logs to another replica every 30 seconds
	*/
	private class SendGossipMsgTask extends TimerTask {

    	private String replicPath;
    	private ZKNaming zkNaming;
    	private ZKRecord record;
    	private SiloGrpc.SiloBlockingStub stub;

		public SendGossipMsgTask(String zooHost, String zooPort, String replicPath) {
			this.replicPath = replicPath;
			this.zkNaming = new ZKNaming(zooHost, zooPort);
		}

		public void reconect() {
			/*
				Try to connect to a random replica
			*/
			try {
				
				// list all registered replicas in /grpc/sauron/silo
				List<ZKRecord> records = (ArrayList<ZKRecord>) this.zkNaming.listRecords("/grpc/sauron/silo");
				Random rand = new Random();

				// if there isnt another registered replica abort
				if (records.size() <= 1) {
					return;
				}

				// else try to choose one different from itself
				do {
					int recordIndex = rand.nextInt(records.size());
					this.record = records.get(recordIndex);
				} while (this.record.getPath().equals(this.replicPath));

				String target = this.record.getURI();

				final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

				this.stub = SiloGrpc.newBlockingStub(channel);

			} catch (ZKNamingException e) { System.err.println(e); }
		}

		@Override
		public void run() {

			// Conect to a random replica
			this.reconect();

			// try to send the gossip message to a random replica, with 3 attempts
			for (int i = 0; i < MAX_ATTEMPTS; i++) {
				try {

					// extract the given replica number
					String[] splitedPath = this.record.getPath().split("/");
					Integer  replicToSendNumber = Integer.parseInt(splitedPath[splitedPath.length-1]);

					// get that replica timestamp
					List<Integer> timestamp = timestampTable.get(replicToSendNumber - 1);
					List<UpdateLog> logsToSend = new ArrayList<>();

					// Predict usefull logs for that replica
					updateLogs.stream()
						.forEach(log -> {
							if(!biggerThan(timestamp, log.getTimestampList())){
								logsToSend.add(log);
							}
						}
					);

					GossipRequest request = GossipRequest.newBuilder()
												.addAllReplicTimestamp(replicTimestamp)
												.addAllUpdateLogs(logsToSend)
												.setReplicNumber(replicNumber)
												.build();

					GossipResponse response = this.stub.receiveGossipMsg(request);

					// update the timestamp table with the value receive from that replica
					timestampTable.set(replicToSendNumber - 1, new ArrayList<>(response.getTimestampList()));
					System.out.println("sent: " + logsToSend);
					return;
				}
				catch (StatusRuntimeException e) {
					this.reconect();
				}
			}
			// if the 3 attempts failed assume that there isnt more replicas
			System.out.println("Couldn't reach another replica...");  	
		}
	}

	/*
		Check if the client timestamp is bigger than this replica,
		if so server cant reply to this client
	*/
	private List<Integer> query(List<Integer> prevTimestamp) {

		for (int i = 0; i < prevTimestamp.size(); i++)
			if (prevTimestamp.get(i) > this.replicTimestamp.get(i))
				return null;

		return this.replicTimestamp;
	}

	@Override
	public void receiveGossipMsg(GossipRequest request, StreamObserver<GossipResponse> responseObserver) {
			
		GossipResponse.Builder responseBuilder = GossipResponse.newBuilder();
		
		// extract the replica number that is sending the gossip msg
		Integer replicNum = request.getReplicNumber();

		// extract the replica timestamp that is sending the gossip msg
		List<Integer> ts = request.getReplicTimestampList();
		if(ts == null)
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Timestamp cannot be empty!")
					.asRuntimeException());
		
		// extract the replica timestamp that is sending the gossip msg
		List<UpdateLog> updateLogs = request.getUpdateLogsList();
		if(updateLogs == null)
			responseObserver.onError(INVALID_ARGUMENT.withDescription("UpdateLog cannot be empty!")
					.asRuntimeException());
		
		// update the timestamp table with the replica timestamp
		this.timestampTable.set(replicNum-1, ts);

		// for each update log receive
		updateLogs.stream()
		.forEach(log -> {
			// check if its a new log for this replica
			if(!biggerThan(this.replicTimestamp,log.getTimestampList())) {

				// add the log to its updates
				this.updateLogs.add(log);
				
				// add the log content to the Silo
				switch(log.getType()) {
					case EYE:
						Camera eye = log.getEye();
						System.out.println("joining eye: " + eye.getCamName());
						try {
							silo.camJoin(eye.getCamName(), eye.getLatitude(), eye.getLongitude());
						}
						catch (SiloException e) {
							System.err.println("Caught exception when reporting:" + e);
						}
						break;

					case OBSERVATION:
						System.out.println("joining report: " + log.getObservationsList());
						List<Observation> obs = log.getObservationsList();
						try {
							silo.report(obs);
						}
						catch(SiloException e) {
							System.err.println("Caught exception when reporting:" + e);	
						}
						break;
				}
			}
		});
		
		// update the replica timestamp with the updated logs received
		for (int i = 0 ; i < N_REPLICS; i++)
			if (this.replicTimestamp.get(i) < ts.get(i))
				this.replicTimestamp.set(i, ts.get(i));
		
		// reply with the new timestamp
		GossipResponse response = responseBuilder.addAllTimestamp(this.replicTimestamp).build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
	
	/*
		Check if the timestamp ts1 is bigger than the ts2
	*/
	private boolean biggerThan(List<Integer> ts1 , List<Integer> ts2) {
		if (ts1.size() == 0) return false;
		if (ts2.size() == 0) return true;
		for(int i=0; i < N_REPLICS; i++)
			if(ts1.get(i) < ts2.get(i))
				return false;
		return true;
	}
	
	@Override
	public void ctrlPing(PingRequest request, StreamObserver<PingResponse> responseObserver) {

		String inputText = request.getInputText();

		if (inputText == null || inputText.isBlank()) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Input cannot be empty!")
							.asRuntimeException());
		}

		String output = "Hello " + inputText + "!";
		PingResponse response = PingResponse.newBuilder()
											.setOutputText(output)
											.build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}
	
	@Override
	public void ctrlClear(ClearRequest request, StreamObserver<ClearResponse> responseObserver) {

		ClearResponse.Builder responseBuilder = ClearResponse.newBuilder();
		silo.ctrlClear();
		
		ClearResponse response = responseBuilder.build();
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void ctrlInit (InitRequest request, StreamObserver<InitResponse> responseObserver) {

		InitResponse response = InitResponse.newBuilder().build();

		// NOT USED

		responseObserver.onNext(response);

		responseObserver.onCompleted();
	}

	@Override
	public void camJoin(CamJoinRequest request, StreamObserver<CamJoinResponse> responseObserver) {

		// extract the client timestamp
		List<Integer> timestamp = new ArrayList<>(request.getPrevTimestampList());

		// extract the camera properties
		String camName   = request.getCamName();
		Double latitude  = request.getLatitude(); 
		Double longitude = request.getLongitude();

		try {

			// try to join the given camera
			boolean newEye = silo.camJoin(request.getCamName(), request.getLatitude(), request.getLongitude());

			// if the camera is new create a new update log with it
			if (newEye) {

				// increment the replica timestamp
				replicTimestamp.set(replicNumber - 1, replicTimestamp.get(replicNumber - 1) + 1);
	
				// create the log timestamp
				timestamp.set(replicNumber - 1, replicTimestamp.get(replicNumber - 1));
			
				Camera eye = Camera.newBuilder()
								.setCamName(camName)
								.setLatitude(latitude)
								.setLongitude(longitude)
								.build();

				UpdateLog updateLog = UpdateLog.newBuilder()
										.setType(UpdateType.EYE)
										.addAllTimestamp(timestamp)
										.setEye(eye)
										.build();

				updateLogs.add(updateLog);
			}
		}
		catch (SiloException siloException) {
			
			switch (siloException.getErrorMessage()) {

				case INVALID_COORDS:
					responseObserver.onError(Status.INVALID_ARGUMENT.withDescription("Invalid coords").asRuntimeException());
					break;
					
				case EYE_ALREADY_EXISTS:
					responseObserver.onError(Status.ALREADY_EXISTS.withDescription("That camera name already exists").asRuntimeException());
					break;
			}
		}
		
		// reply with the new client timestamp
		CamJoinResponse response = CamJoinResponse.newBuilder()
										.addAllNextTimestamp(timestamp)
										.build();

		responseObserver.onNext(response);
		
		responseObserver.onCompleted();
	}

	@Override
	public void camInfo(CamInfoRequest request, StreamObserver<CamInfoResponse> responseObserver) {

		Coords coords = null;
		List<Integer> nextTs = null;

		CamInfoResponse response;

		try {

			// Check if the client timestamp is bigger than this replica
			nextTs = this.query(request.getPrevTimestampList());

			// if not reply
			if (nextTs != null) {
				coords = silo.camInfo(request.getCamName());
			} else {
				responseObserver.onError(Status.UNAVAILABLE.withDescription("Server needs update before giving a response. Try again later.").asRuntimeException());
			}
		}
		// if the given camera name doesnt exist
		catch (SiloException siloException) {
			responseObserver.onError(Status.NOT_FOUND.withDescription("NOT_FOUND").asRuntimeException());
		}

		// reply with the new client timestamp and the camera coords
		response = CamInfoResponse.newBuilder()
					.setLatitude(coords.getLatitude())
					.setLongitude(coords.getLongitude())
					.addAllNextTimestamp(nextTs)
					.build();
	
		responseObserver.onNext(response);

		responseObserver.onCompleted();
	}

	@Override
	public void report(ReportRequest request, StreamObserver<ReportResponse> responseObserver) {

		// extract the client timestamp
		List<Integer> timestamp = new ArrayList<>(request.getPrevTimestampList());
		
		// extract the given observations to report
		int observations = request.getObservationsCount();
		if (observations == 0) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("There are no observations in this report!")
							.asRuntimeException());
		}
		
		// extract the given camera name
		String camName = request.getObservations(0).getCamName();
		if (camName == null || camName.isBlank()) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Eye name cannot be empty!")
							.asRuntimeException());
		}
		
		// for eache given observation
		request.getObservationsList().stream()
			.forEach(observation -> {

				// Check if theres a datetime
				Timestamp dateTime = observation.getDateTime();
				if (dateTime.getSeconds() == 0)
					responseObserver.onError(INVALID_ARGUMENT.withDescription("There is no timestamp in an observation!")
						.asRuntimeException());

				// And if its the correct camera name
				if (!(observation.getCamName().equals(camName)))
					responseObserver.onError(INVALID_ARGUMENT.withDescription("Multiple cameras reffered!")
							.asRuntimeException());
			}
		);
		
		ReportResponse.Builder responseBuilder = ReportResponse.newBuilder();

		try {
			// Report the observations to the silo and get the executed ones
			List<Observation> obs = silo.report(request.getObservationsList());

			// Update the replica timestamp
			replicTimestamp.set(replicNumber - 1, replicTimestamp.get(replicNumber - 1) + 1);

			// Create the timestamp for this log
			timestamp.set(replicNumber - 1, replicTimestamp.get(replicNumber - 1));
		
			UpdateLog updateLog = UpdateLog.newBuilder()
									.setType(UpdateType.OBSERVATION)
									.addAllTimestamp(timestamp)
									.addAllObservations(obs)
									.build();

			// Add this log to the updateLogs
			updateLogs.add(updateLog);
		}
		catch(SiloException e) {

			switch(e.getErrorMessage()) {

			case INVALID_TARGET_ID :
				responseObserver.onError(INVALID_ARGUMENT.withDescription("Target id is invalid!")
						.asRuntimeException());
				break;

			case EYE_NOT_FOUND :
				responseObserver.onError(NOT_FOUND.withDescription("Eye does not exist!")
						.asRuntimeException());
				break;

			case INVALID_TARGET_TYPE :
				responseObserver.onError(INVALID_ARGUMENT.withDescription("Target type does not exist!")
						.asRuntimeException());
				break;

			case INVALID_TIMESTAMP :
				responseObserver.onError(INVALID_ARGUMENT.withDescription("Target type does not exist!")
						.asRuntimeException());
				break;
			}
		}
		
		// reply with the new client timestamp
		ReportResponse response = responseBuilder.addAllNextTimestamp(timestamp).build();

		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void track(TrackRequest request, StreamObserver<TrackResponse> responseObserver) {

		// extract the client timestamp
		List<Integer> timestamp = new ArrayList<>(request.getPrevTimestampList());
		List<Integer> nextTs = null;

		// extract the given target type
		TargetType targetType = request.getTargetType();
		if (targetType == null) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("TargetType cannot be empty!")
							.asRuntimeException());
		}

		// extract the given target Id
		String targetId = request.getTargetId();
		if (targetId == null || targetId.isBlank()) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Target id cannot be empty!")
							.asRuntimeException());
		}

		TrackResponse.Builder responseBuilder = TrackResponse.newBuilder();

		try {

			// Check if the client timestamp is bigger than this replica
			nextTs = this.query(timestamp);

			// if not reply
			if (nextTs != null) {
				responseBuilder.setObservation(silo.track(targetType, targetId));
			} else {
				responseObserver.onError(Status.UNAVAILABLE.withDescription("Server needs update before giving a response. Try again later.").asRuntimeException());
			}

		} catch (SiloException e) {

			switch (e.getErrorMessage()) {

				case TARGET_NOT_FOUND:
					responseObserver.onError(
								NOT_FOUND.withDescription("Target not found!")
									.asRuntimeException());
					break;

				case INVALID_TARGET_TYPE:
					responseObserver.onError(
								INVALID_ARGUMENT.withDescription("Invalid target type!")
									.asRuntimeException());
					break;
			}
		}

		// reply with the new client timestamp and the respective observation
		TrackResponse response = responseBuilder.addAllNextTimestamp(nextTs).build();
	
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

	@Override
	public void trackMatch(TrackMatchRequest request, StreamObserver<TrackMatchResponse> responseObserver) {

		// extract the client timestamp
		List<Integer> timestamp = new ArrayList<>(request.getPrevTimestampList());
		List<Integer> nextTs = null;

		// extract the given target type
		TargetType targetType = request.getTargetType();
		if (targetType == null) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("TargetType cannot be empty!")
							.asRuntimeException());
		}

		// extract the given target Id
		String targetId = request.getTargetId();
		if (targetId == null || targetId.isBlank()) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Target id cannot be empty!")
							.asRuntimeException());
		}

		TrackMatchResponse.Builder responseBuilder = TrackMatchResponse.newBuilder();

		try {

			// Check if the client timestamp is bigger than this replica
			nextTs = this.query(timestamp);

			// if not reply
			if (nextTs != null) {
				silo.trackMatch(targetType, targetId).stream()
					.forEach(observation -> responseBuilder.addObservations(observation));
			} else {
				responseObserver.onError(Status.UNAVAILABLE.withDescription("Server needs update before giving a response. Try again later.").asRuntimeException());

			}

		} catch (SiloException e) {

			System.err.println("Caught exception when reporting:" + e);

			switch (e.getErrorMessage()) {

				case INVALID_TARGET_ID:
					responseObserver.onError(
								INVALID_ARGUMENT.withDescription("Id not valid!")
									.asRuntimeException());
					break;

				case INVALID_TARGET_TYPE:
					responseObserver.onError(
								INVALID_ARGUMENT.withDescription("Invalid target type!")
									.asRuntimeException());
					break;
			}
		}

		// reply with the new client timestamp and the respective observations
		TrackMatchResponse response = responseBuilder.addAllNextTimestamp(nextTs).build();
	
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}	

	@Override
	public void trace(TraceRequest request, StreamObserver<TraceResponse> responseObserver) {

		// extract the client timestamp
		List<Integer> timestamp = new ArrayList<>(request.getPrevTimestampList());
		List<Integer> nextTs = null;

		// extract the given target type
		TargetType targetType = request.getTargetType();
		if (targetType == null) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("TargetType cannot be empty!")
							.asRuntimeException());
		}

		// extract the given target Id
		String targetId = request.getTargetId();
		if (targetId == null || targetId.isBlank()) {
			responseObserver.onError(INVALID_ARGUMENT.withDescription("Target id cannot be empty!")
							.asRuntimeException());
		}

		TraceResponse.Builder responseBuilder = TraceResponse.newBuilder();

		try {

			// Check if the client timestamp is bigger than this replica
			nextTs = this.query(request.getPrevTimestampList());

			// if not reply
			if (nextTs != null) {
				silo.trace(targetType, targetId).stream()
					.forEach(observation -> responseBuilder.addObservations(observation));
			} else {
				responseObserver.onError(Status.UNAVAILABLE.withDescription("Server needs update before giving a response. Try again later.").asRuntimeException());
			}

		} catch (SiloException e) {

			System.err.println("Caught exception when reporting:" + e);

			switch (e.getErrorMessage()) {
				case INVALID_TARGET_ID:
					responseObserver.onError(
                            INVALID_ARGUMENT.withDescription("Invalid target id!")
                                .asRuntimeException());
					break;
				case TARGET_NOT_FOUND:
					responseObserver.onError(
								NOT_FOUND.withDescription("Target not found!")
									.asRuntimeException());
					break;

				case INVALID_TARGET_TYPE:
					responseObserver.onError(
								INVALID_ARGUMENT.withDescription("Invalid target type!")
									.asRuntimeException());
					break;
			}
		}

		// reply with the new client timestamp and the respective observations
		TraceResponse response = responseBuilder.addAllNextTimestamp(nextTs).build();
	 
		responseObserver.onNext(response);
		responseObserver.onCompleted();
	}

}
