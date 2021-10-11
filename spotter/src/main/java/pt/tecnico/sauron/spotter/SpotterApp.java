package pt.tecnico.sauron.spotter;

import pt.tecnico.sauron.silo.client.SiloFrontend;
import pt.tecnico.sauron.silo.grpc.CamInfoRequest;
import pt.tecnico.sauron.silo.grpc.CamInfoResponse;
import pt.tecnico.sauron.silo.grpc.ClearRequest;
import pt.tecnico.sauron.silo.grpc.ClearResponse;
import pt.tecnico.sauron.silo.grpc.Observation;
import pt.tecnico.sauron.silo.grpc.PingRequest;
import pt.tecnico.sauron.silo.grpc.PingResponse;
import pt.tecnico.sauron.silo.grpc.TargetType;
import pt.tecnico.sauron.silo.grpc.TraceRequest;
import pt.tecnico.sauron.silo.grpc.TrackMatchRequest;
import pt.tecnico.sauron.silo.grpc.TrackRequest;
import io.grpc.StatusRuntimeException;
import java.util.Comparator;
import java.util.stream.Collectors;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;

import com.google.protobuf.util.Timestamps;

import java.io.IOException; 
 

public class SpotterApp {
	
	public static void main(String[] args) throws IOException {
		boolean exit_flag=false;
		System.out.println(SpotterApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}
		
		// check arguments
		if (args.length != 3) {
			System.out.println("Wrong Arguments!");
			return;
		}
		
		final String zooHost = args[0];
		final String zooPort = args[1];
		final String path    = args[2];
		
		
		List<Integer> timestamp;
		SiloFrontend frontend;
		String line = null;
		String[] splitedPath = path.split("/");
		Integer replicNumber = Integer.parseInt(splitedPath[splitedPath.length-1]);
		if(replicNumber != -1)
			frontend = new SiloFrontend(zooHost, zooPort, path);
		else
			frontend = new SiloFrontend(zooHost, zooPort);
		Scanner scanner = new Scanner(System.in);
		TargetType tipo= null;
		
		while(exit_flag==false) {
			if(!scanner.hasNextLine())
				break;
			if((line = scanner.nextLine()).isEmpty())
				continue;
			
			String token = new String();
			String output= new String();
			StringTokenizer tokenizer = new StringTokenizer(line);
			
			switch(tokenizer.nextToken()) {
			
			case "spot":
				if(tokenizer.countTokens()!=2) {
					System.out.println("Invalid number of arguments for command spot"); 
					break;
				}
				token=tokenizer.nextToken();
				if(token.compareTo("person") == 0)
					 tipo = TargetType.PERSON;
				
				else if(token.compareTo("car") == 0)
					 tipo = TargetType.CAR;
				
				else {
					System.out.println("Unkown Type in command spot.");
					break;
				}
				String trackId = tokenizer.nextToken();
				if(trackId.indexOf('*')==-1) {
					doTrack(frontend, tipo, trackId);

					
				}
				else {
					doTrackMatch(frontend, tipo, trackId);
				}
				
				break;
			case "clear":
				ClearRequest  clear_request  = ClearRequest.newBuilder().build();
				frontend.ctrlClear(clear_request);
				System.out.println("Server cleared");
				break;
				
			case "ping":
				PingRequest request = PingRequest.newBuilder().setInputText("friend").build();
				PingResponse response = frontend.ctrlPing(request);
				System.out.println(response.getOutputText());
				break;
				
			case "help":
				System.out.println("Valid Commands: \n"
						+          "spot - 	Send location of last time target was seen. Recebe um tipo e um id. Ex:spot car 11DD90 \n"
						+ 			"trail - Send in chronological order the places target were spotted. Recebe um tipo e um id.Ex: trail person 11235476 \n"
						+           "help -Display this.\n"
						+           "clear - Clear data in server\n"
						+           "ping - ping the server\n"
						+           "exit - Quit spotter\n");
				break;
				
			case "trail":
				
				if(tokenizer.countTokens()!=2) {
					System.out.println("Invalid number of arguments for command trail"); 
					break;
				}
				
				token=tokenizer.nextToken();
				if(token.compareTo("person") == 0)
					 tipo = TargetType.PERSON;
				
				else if(token.compareTo("car") == 0)
					 tipo = TargetType.CAR;
				
				else {
					System.out.println("Unkown Type in command trail.");
					break;
				}
				
				String trailId = tokenizer.nextToken();
				
				doTrail(frontend, tipo, trailId);		
					
				break;
			case "exit":
				exit_flag=true;
				break;
			default:
				System.out.println("Command not found.\n");
			
			}

		}
		scanner.close();
		
		
	}

	private static  void doTrail(SiloFrontend frontend, TargetType tipo, String trailId) {
		try {
			TraceRequest traceRequest = TraceRequest.newBuilder()
					.setTargetType(tipo)
					.setTargetId(trailId)
					.build();
			List <Observation> observations = frontend.trace(traceRequest).getObservationsList();
			observations.stream()
			.forEach(observation -> {
				String camName = observation.getCamName();
				CamInfoRequest camInfoRequest = CamInfoRequest.newBuilder()
						.setCamName(camName)
						.build();
				CamInfoResponse camInfoResponse = frontend.camInfo(camInfoRequest);
				String coordenates =String.valueOf(camInfoResponse.getLongitude())+','+String.valueOf(camInfoResponse.getLatitude());
				String type = null;
				if(observation.getTargetType()==TargetType.PERSON)
					type="person";
				else
					type="car";
				
				System.out.println(type+','+observation.getTargetId()+','+Timestamps.toString(observation.getDateTime())+','+camName+','+coordenates);
			});
		}
		
		catch(StatusRuntimeException e){
			System.out.println("Server caught exception with description: " + 
					e.getStatus().getDescription());
		}
	}

	private static  void doTrackMatch(SiloFrontend frontend, TargetType tipo, String trackId) {
		try {
			TrackMatchRequest trackMatchRequest = TrackMatchRequest.newBuilder()
					.setTargetType(tipo)
					.setTargetId(trackId)
					.build();
		List <Observation> observations =	frontend.trackMatch(trackMatchRequest).getObservationsList();
		Comparator<Observation> idComparator = Comparator.comparing(Observation::getTargetId);
		List<Observation> outputList = observations.stream()
		.sorted(idComparator)
		.collect(Collectors.toList());
		outputList.stream()
			.forEach(observation -> {
				String camName = observation.getCamName();
				CamInfoRequest camInfoRequest = CamInfoRequest.newBuilder()
						.setCamName(camName)
						.build();
				CamInfoResponse camInfoResponse = frontend.camInfo(camInfoRequest);
				String coordenates =String.valueOf(camInfoResponse.getLongitude())+','+String.valueOf(camInfoResponse.getLatitude());
				String type = null;
				if(observation.getTargetType()==TargetType.PERSON)
					type="person";
				else
					type="car";
				
				System.out.println(type+','+observation.getTargetId()+','+Timestamps.toString(observation.getDateTime())+','+camName+','+coordenates);
			});

		
		
		
		}
		catch(StatusRuntimeException e){
			System.out.println("Server caught exception with description: " + 
					e.getStatus().getDescription());
		}
	}

	private static  void doTrack(SiloFrontend frontend, TargetType tipo, String trackId) {
		try {
			TrackRequest trackRequest = TrackRequest.newBuilder()
					.setTargetType(tipo)
					.setTargetId(trackId)
					.build();
			Observation observation =	frontend.track(trackRequest).getObservation();
			String camName = observation.getCamName();
			CamInfoRequest camInfoRequest = CamInfoRequest.newBuilder()
					.setCamName(camName)
					.build();
			CamInfoResponse camInfoResponse = frontend.camInfo(camInfoRequest);
			String coordenates =String.valueOf(camInfoResponse.getLongitude())+','+String.valueOf(camInfoResponse.getLatitude());
			String type = null;
			if(observation.getTargetType()==TargetType.PERSON)
				type="person";
			else
				type="car";
			
			System.out.println(type+','+observation.getTargetId()+','+Timestamps.toString(observation.getDateTime())+','+camName+','+coordenates);
			
		}
		catch(StatusRuntimeException e){
		    System.out.println("Server caught exception with description: " + 
		    		e.getStatus().getDescription());
			
		}
	}

}
