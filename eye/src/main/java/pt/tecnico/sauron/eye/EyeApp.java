package pt.tecnico.sauron.eye;

import java.util.Scanner;

import com.google.protobuf.Timestamp;

import java.io.IOException;
import java.io.BufferedReader; 
import java.io.InputStreamReader;
import java.time.Instant;

import io.grpc.Status;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

import pt.tecnico.sauron.silo.grpc.*;
import pt.tecnico.sauron.silo.client.SiloFrontend;

public class EyeApp {

	private static SiloFrontend siloFrontend;

	public static void main (String[] args) throws IOException {

		final String         zooHost;
		final String         zooPort;
		final String         path;
		final String         target;
		final String         eyeName;
		final Double         eyeLatitude;
		final Double         eyeLongitude;
		final ManagedChannel channel;
		
		CamJoinRequest  camJoinRequest;
		CamJoinResponse camJoinResponse; 
		
		System.out.println(EyeApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// check arguments
		if (args.length < 6) {
			System.out.println("Argument(s) missing!");
			System.out.printf("Usage: java %s zooHost zooPort path eyeName eyeLatitude eyeLongitude%n", EyeApp.class.getName());
			return;
		}

		zooHost      = args[0];
		zooPort      = args[1];
		path         = args[2];
		eyeName      = args[3];
		eyeLatitude  = Double.parseDouble(args[4]);
		eyeLongitude = Double.parseDouble(args[5]);

		String[] splitedPath = path.split("/");
		Integer replicNumber = Integer.parseInt(splitedPath[splitedPath.length-1]);
		if(replicNumber != -1)
			siloFrontend = new SiloFrontend(zooHost, zooPort, path);
		else
			siloFrontend = new SiloFrontend(zooHost, zooPort);

		camJoinRequest = CamJoinRequest.newBuilder().setCamName(eyeName).setLatitude(eyeLatitude).setLongitude(eyeLongitude).build();

		try {
			camJoinResponse = siloFrontend.camJoin(camJoinRequest);
		}
		catch (StatusRuntimeException e) {
			Status status = e.getStatus();
        	System.out.println(status.getDescription());
        	System.exit(0);
		}
		executeEye(eyeName);
	}
	
	private static void executeEye (String eyeName) throws IOException {

		int ms;

		String line = null;

		String[] tokens;

		Observation observation;

		ReportRequest.Builder reportRequestBuilder;

		ReportRequest reportRequest;

		ReportResponse reportResponse;

		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

		boolean startedReport = false;

		int reportsNumber = 0;

			do {

				System.out.println("Waiting for a report...");

				reportRequestBuilder = ReportRequest.newBuilder();

				while (true) {
				
					line = reader.readLine();

					if (line == null) {
						System.exit(0);
					}
					
					else if (line.isEmpty()) {
						if (startedReport && reportsNumber > 0) {
							reportRequest  = reportRequestBuilder.build();
							try {
								reportResponse = siloFrontend.report(reportRequest); 
							}
							catch (StatusRuntimeException e) {
								Status status = e.getStatus();
        						System.out.println(status.getDescription());
								break;
							}
							System.out.println("Report successfully commited");
							startedReport = false;
							reportsNumber = 0;
							break;
						}
					}
					else if (line.equals("help")) {
						System.out.println("To report a car/person use: 'car/person,<carId>/<personId>'");
						System.out.println("To delay commands use: 'zzz,<ms>' ");
						System.out.println("To submit your report press ENTER");
					}
 					else if (line.charAt(0) == '#') {
						continue;
					}
					else {
						tokens = line.split(",");

						if (tokens[0].equals("zzz")) {
							ms = Integer.parseInt(tokens[1]);
							try {
								Thread.sleep(ms);
							} 
							catch (Exception e) {
								System.out.println(e);
							}
						}
						else {
							if (tokens[0].equals("person")) {
								startedReport = true;
								reportsNumber++;
								Instant time = Instant.now();
								Timestamp dateTime = Timestamp.newBuilder().setSeconds(time.getEpochSecond())
								    .setNanos(time.getNano()).build();
								observation =  Observation.newBuilder().setDateTime(dateTime).setCamName(eyeName).setTargetType(TargetType.PERSON).setTargetId(tokens[1]).build();
								reportRequestBuilder.addObservations(observation);
							}
							else if (tokens[0].equals("car")) {
								reportsNumber++;
								startedReport = true;
								Instant time = Instant.now();
								Timestamp dateTime = Timestamp.newBuilder().setSeconds(time.getEpochSecond())
								    .setNanos(time.getNano()).build();
								observation =  Observation.newBuilder().setDateTime(dateTime).setCamName(eyeName).setTargetType(TargetType.CAR).setTargetId(tokens[1]).build();	
								reportRequestBuilder.addObservations(observation);
							}
							if (startedReport && reportsNumber == 1) {
								System.out.println("You have started a report");
								System.out.println("Press enter to submit report"); 
							}
						}
					}
				}

			} while (true);
		}
}
