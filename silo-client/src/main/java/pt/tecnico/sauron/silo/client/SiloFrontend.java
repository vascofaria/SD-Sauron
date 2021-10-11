package pt.tecnico.sauron.silo.client;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.Collectors;

import com.google.protobuf.util.Timestamps;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

import pt.tecnico.sauron.silo.grpc.*;

import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKRecord;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

public class SiloFrontend {
	
	private static final Integer N_REPLICS = 10;
	private static final Integer MAX_ATTEMPTS = 3;

	private ZKNaming zkNaming;

	private SiloGrpc.SiloBlockingStub stub;

	private List<Integer> timestamp = new ArrayList<>();
	private boolean fixedServer = false;

	private Camera iEye;

	private Map<String, Camera>  cachedEyes = new HashMap<>();
	private Map<String, List<Observation>>  cachedObservations   = new HashMap<>();
	
	public SiloFrontend(String zooHost, String zooPort, String path) {

		this.fixedServer = true;

		for (int i =  0; i < N_REPLICS; i++) {
			this.timestamp.add(0);
		}

		try {
			this.zkNaming = new ZKNaming(zooHost, zooPort);

			// lookup
			ZKRecord record = this.zkNaming.lookup(path);
			String   target = record.getURI();

			final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

			this.stub = SiloGrpc.newBlockingStub(channel);

		} catch (ZKNamingException e) { System.err.println(e); }
		
	}
	
	public SiloFrontend(String zooHost, String zooPort) {

		for (int i =  0; i < N_REPLICS; i++) {
			this.timestamp.add(0);
		}

		try {
			this.zkNaming = new ZKNaming(zooHost, zooPort);

			// lookup
			List<ZKRecord> records = (ArrayList<ZKRecord>) this.zkNaming.listRecords("/grpc/sauron/silo");
			// pick random server from list
			Random rand = new Random();
			int serverNum = rand.nextInt(records.size());	
			ZKRecord record = records.get(serverNum);
			String   target = record.getURI();

			final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

			this.stub = SiloGrpc.newBlockingStub(channel);

		} catch (ZKNamingException e) { System.err.println(e); }
		
	}

	public void reconect() {
		try {

			// lookup
			List<ZKRecord> records = (ArrayList<ZKRecord>) this.zkNaming.listRecords("/grpc/sauron/silo");
			// pick random server from list
			Random rand = new Random();
			int serverNum = rand.nextInt(records.size());
			ZKRecord record = records.get(serverNum);
			String   target = record.getURI();

			final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

			this.stub = SiloGrpc.newBlockingStub(channel);

		} catch (ZKNamingException e) { System.err.println(e); }
	}

	public PingResponse ctrlPing(PingRequest request) {
		return this.stub.ctrlPing(request);
	}
	
	public ClearResponse ctrlClear(ClearRequest request) {
		return this.stub.ctrlClear(request);
	}

	public InitResponse ctrlInit(InitRequest request) {
		return this.stub.ctrlInit(request);
	}

	public CamJoinResponse camJoin(CamJoinRequest request) {
		// records which camera it is
		this.iEye = Camera.newBuilder()				
						.setCamName(request.getCamName())
						.setLatitude(request.getLatitude())
						.setLongitude(request.getLongitude())
						.build();
		// creates request with timestamp included
		CamJoinRequest newRequest = CamJoinRequest.newBuilder()		
				.addAllPrevTimestamp(this.timestamp)
				.setCamName(request.getCamName())
				.setLatitude(request.getLatitude())
				.setLongitude(request.getLongitude()).build();

		CamJoinResponse response = null;
		
		// tries to do request 3 times or until it succeeds
		for (int i = 0; i < MAX_ATTEMPTS; i++) {
			try {
				response = this.stub.camJoin(newRequest);
				//update timestamp
				this.timestamp = new ArrayList<>(response.getNextTimestampList());    
				return response;
			} catch (StatusRuntimeException e ) { 
				if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
					if (!this.fixedServer)
						// if server is behind client or server is down connect to other random server
						this.reconect();
				}
				else {
					throw e;
				}
			}
		}
		
		throw  Status.NOT_FOUND.withDescription("Servers Unavailable").asRuntimeException();

	}

	public CamInfoResponse camInfo(CamInfoRequest request) {
		CamInfoRequest newRequest = CamInfoRequest.newBuilder()
			.addAllPrevTimestamp(this.timestamp)
			.setCamName(request.getCamName())
			.build();

		CamInfoResponse response = null;

		// try to use function 3 times or until it has been done successfully
		for (int i = 0; i < MAX_ATTEMPTS ; i++) {
			try {
				response = this.stub.camInfo(newRequest);
				// update timestamp
				this.timestamp = new ArrayList<>(response.getNextTimestampList());    
				// cache response
				Camera camera = Camera.newBuilder()
						.setCamName(newRequest.getCamName())
						.setLatitude(response.getLatitude())
						.setLongitude(response.getLongitude())
						.build();
				this.cachedEyes.put(camera.getCamName(),camera);	
				return response;
			} catch (StatusRuntimeException e) {
				// if server was down or server is behind try other server
				if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
					if (!this.fixedServer)		
						this.reconect();
				}
				else 						
					throw e;
			} catch (Exception e) {
				System.out.println("Trying to reconect...");
			}
		}
		// try to get cached response
		if(this.cachedEyes.containsKey(newRequest.getCamName())) {	
			Camera camera = this.cachedEyes.get(newRequest.getCamName());
			response = CamInfoResponse.newBuilder()
					.setLatitude(camera.getLatitude())
					.setLongitude(camera.getLongitude())
					.build();						
		}
		else 
			throw  Status.NOT_FOUND.withDescription("NOT_FOUND").asRuntimeException();
			
		return response;
	}

	public ReportResponse report(ReportRequest request) {
		// login camera
		CamJoinRequest camRequest = CamJoinRequest.newBuilder()				
									.setCamName(this.iEye.getCamName())
									.setLatitude(this.iEye.getLatitude())
									.setLongitude(this.iEye.getLongitude())
									.build();

		this.camJoin(camRequest);
		// add timestamp to request
		ReportRequest newRequest = ReportRequest.newBuilder()			
				.addAllObservations(request.getObservationsList())
				.addAllPrevTimestamp(this.timestamp).build();

		ReportResponse response = null;
		for (int i = 0; i < MAX_ATTEMPTS ; i++) {
			try {
				response = this.stub.report(newRequest);
				// update timestamp
				this.timestamp = new ArrayList<>(response.getNextTimestampList());    
				return response;
			} catch (StatusRuntimeException e) {
				// if server was down or server is behind try other server
				if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
					if (!this.fixedServer)		
						this.reconect();
				}
				else
					throw e;
			} catch (Exception e) {
				System.out.println("Trying to reconect...");
			}
		}
		throw Status.NOT_FOUND.withDescription("NOT_FOUND").asRuntimeException();
	}

	public TrackResponse track(TrackRequest request) {

		TrackRequest newRequest = TrackRequest.newBuilder()
				.setTargetId(request.getTargetId())
				.setTargetType(request.getTargetType())
				.addAllPrevTimestamp(this.timestamp).build();

		TrackResponse response = null;
		// try to use function 3 times or until it has been done successfully
		for (int i = 0; i < MAX_ATTEMPTS ; i++) {
			try {
				response = this.stub.track(newRequest);
				// update time stamp
				this.timestamp = new ArrayList<>(response.getNextTimestampList());
				// cache response
				if(cachedObservations.containsKey(response.getObservation().getTargetId())) {	
					List<Observation> observationsList = cachedObservations.get(response.getObservation().getTargetId());
					if(!(observationsList.get(observationsList.size()-1).equals(response.getObservation()))){
						cachedObservations.get(response.getObservation().getTargetId()).add(response.getObservation());
					}
				}
				else {
					List<Observation> observationList = new ArrayList<>();
					observationList.add(response.getObservation());
					cachedObservations.put(response.getObservation().getTargetId(), observationList);
				}
				return response;
			} catch (StatusRuntimeException e) {
				// if server was down or server is behind try other server
				if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
					if (!this.fixedServer)
						this.reconect();
				}
				else {
					throw e;
				}
			} catch (Exception e) {
				System.out.println("Trying to reconect...");
			}
		}
		// try to get cached response
		if(cachedObservations.containsKey(request.getTargetId())) {					
			List<Observation> observations = this.cachedObservations.get(request.getTargetId());
			Observation observation = observations.get(observations.size()-1);
			response = TrackResponse.newBuilder()
					.setObservation(observation)
					.build();
			return response;
		}
		else 
			throw  Status.NOT_FOUND.withDescription("NOT_FOUND").asRuntimeException();
	}

	public TrackMatchResponse trackMatch(TrackMatchRequest request) {

		TrackMatchRequest newRequest = TrackMatchRequest.newBuilder()
				.setTargetId(request.getTargetId())
				.setTargetType(request.getTargetType())
				.addAllPrevTimestamp(this.timestamp).build();

		TrackMatchResponse response = null;
		// try to use function 3 times or until it has been done successfully
		for (int i = 0; i < MAX_ATTEMPTS ; i++) {
			try {
				response = this.stub.trackMatch(newRequest);
				// update timestamp
				this.timestamp = new ArrayList<>(response.getNextTimestampList());  
				// cache response
				response.getObservationsList().forEach(observation ->{				
					if(cachedObservations.containsKey(observation.getTargetId())) {
						List<Observation> observationsList = cachedObservations.get(observation.getTargetId());
						if(!(observationsList.get(observationsList.size()-1).equals(observation)))
							cachedObservations.get(observation.getTargetId()).add(observation);
					}
					else {
						List<Observation> list = new ArrayList<>();
						list.add(observation);
						cachedObservations.put(observation.getTargetId(),list);
					}
				});
				return response;
			} catch (StatusRuntimeException e) {
				// if server was down or server is behind try other server		
				if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
					if (!this.fixedServer)		
						this.reconect();
				}
				else 						
					throw e;
			}
		}
		// try to get cached response
		List<Observation> observationsList = new ArrayList<>();
		cachedObservations.forEach((id,observations) ->{
			if(observations.get(observations.size()-1).getTargetType()==request.getTargetType()) {	
				char[] chars = id.toCharArray();
				StringBuilder sb = new StringBuilder();
				for (char c : chars) {
					if (c >= '0' && c <= '9') sb.append(c);
					else if (c == '*') sb.append("(.*)");
					else throw Status.INVALID_ARGUMENT.withDescription("Invalid Target id : " + id).asRuntimeException();
				}
				if(id.toString().matches(sb.toString()))
					observationsList.add(observations.get(observations.size()-1));
			}
		});
		response = TrackMatchResponse.newBuilder().addAllObservations(observationsList).build();
		return response;
	}

	public TraceResponse trace(TraceRequest request) {

		TraceRequest newRequest = TraceRequest.newBuilder()
				.setTargetId(request.getTargetId())
				.setTargetType(request.getTargetType())
				.addAllPrevTimestamp(this.timestamp).build();

		TraceResponse response = null;
		// try to use function 3 times or until it has been done successfully
		for (int i = 0; i < MAX_ATTEMPTS ; i++) {
			try {
				response = this.stub.trace(newRequest);
				// update timestamp
				this.timestamp = new ArrayList<>(response.getNextTimestampList());		
				// cache response
				response.getObservationsList().forEach(observation ->{	
					// if target already exists add new observation and sort by time
					if(cachedObservations.containsKey(observation.getTargetId())) {
						List<Observation> observationsList = cachedObservations.get(observation.getTargetId());
						if(!(observationsList.contains(observation))) {
							cachedObservations.get(observation.getTargetId()).add(observation);
							cachedObservations.get(observation.getTargetId()).stream()
									.sorted((o1,o2) -> Timestamps.between(o1.getDateTime(), o2.getDateTime()).getSeconds()<=0?-1:1);										
						}
					}
					// if target isn't cached yet  add to cache
					else {
						List<Observation> list = new ArrayList<>();
						list.add(observation);
						cachedObservations.put(observation.getTargetId(),list);
					}
				});
				return response;
			} catch (StatusRuntimeException e) {
				// if server was down or server is behind try other server
				if (e.getStatus().getCode().equals(Status.Code.UNAVAILABLE)) {
					if (!this.fixedServer)		
						this.reconect();
				}
				else 						
					throw e;
			}
		}
		// try to get cached response
		if(this.cachedObservations.containsKey(request.getTargetId())) {		
			List<Observation> observationsList = new ArrayList<>();
			observationsList.addAll(cachedObservations.get(request.getTargetId()));
			response = TraceResponse.newBuilder().addAllObservations(observationsList).build();
			return response;
		}
		throw  Status.NOT_FOUND.withDescription("NOT_FOUND").asRuntimeException();		
	}
}
