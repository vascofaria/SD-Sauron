package pt.tecnico.sauron.silo.client;

import pt.tecnico.sauron.silo.grpc.*;
import pt.tecnico.sauron.silo.client.BaseIT;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static io.grpc.Status.NOT_FOUND;
import static io.grpc.Status.INVALID_ARGUMENT;

import com.google.protobuf.Timestamp;

import io.grpc.StatusRuntimeException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class TrackMatchIT extends BaseIT {
	
	private final SiloFrontend frontend = new SiloFrontend("localhost", "2181", "/grpc/sauron/silo/1");


	private static String CAM_NAME = "Tagus";
	private static double LATITUDE = -9.303164; 
	private static double LONGITUE = 38.737613; 
	
	private static String CAM_NAME2 = "Alameda";
	private static double LATITUDE2 = -8.303164; 
	private static double LONGITUE2 = 37.737613; 
	
	
	private static final String VALID_CAR = "DD4341";
	private static final String VALID_PERSON = "12345678";
	private static final String VALID_CAR2 = "DD4342";
	private static final String VALID_PERSON2 = "12345628";
	
	
	private static final String VALID_CAR_SEARCH = "DD43*";
	private static final String VALID_PERSON_SEARCH = "12345*8";

	private static final String UNKNOWN_CAR_SEARCH = "DD3*";
	private static final String UNKNOWN_PERSON_SEARCH = "989*";

	private static final String INVALID_CAR_SEARCH = "D243*D!321";
	private static final String INVALID_PERSON_SEARCH = "12345*8D";

	// one-time initialization and clean-up
	@BeforeAll
	public static void oneTimeSetUp(){


		
	}
	

	@AfterAll
	public static void oneTimeTearDown() {

		
	}
	
	@BeforeEach
	public void setUp() throws InterruptedException {
		ClearRequest  clear_request  = ClearRequest.newBuilder().build();
		frontend.ctrlClear(clear_request);
		CamJoinRequest  request = CamJoinRequest.newBuilder()
												.setCamName(CAM_NAME)
												.setLatitude(LATITUDE)
												.setLongitude(LONGITUE)
												.build();
		frontend.camJoin(request);
		
		CamJoinRequest  request2 = CamJoinRequest.newBuilder()
				.setCamName(CAM_NAME2)
				.setLatitude(LATITUDE2)
				.setLongitude(LONGITUE2)
				.build();
		frontend.camJoin(request2);
		
		
		
		Instant time = Instant.now();
		Timestamp timestamp = Timestamp.newBuilder().setSeconds(time.getEpochSecond())
			    .setNanos(time.getNano()).build();
		Observation observation2 = Observation.newBuilder()
											  .setCamName(CAM_NAME)
											  .setTargetId(VALID_CAR)
											  .setTargetType(TargetType.CAR)
											  .setDateTime(timestamp)
											  .build();
		
		Observation observation1 = Observation.newBuilder()
											  .setCamName(CAM_NAME)
											  .setTargetId(VALID_PERSON)
											  .setTargetType(TargetType.PERSON)
											  .setDateTime(timestamp)
											  .build();

		Observation observation4 = Observation.newBuilder()
											  .setCamName(CAM_NAME2)
											  .setTargetId(VALID_CAR2)
											  .setTargetType(TargetType.CAR)
											  .setDateTime(timestamp)
											  .build();
		
		Observation observation3 = Observation.newBuilder()
											  .setCamName(CAM_NAME2)
											  .setTargetId(VALID_PERSON2)
											  .setTargetType(TargetType.PERSON)
											  .setDateTime(timestamp)
											  .build();

		ReportRequest.Builder  report_requestBuilder  = ReportRequest.newBuilder().addObservations(observation1);
		report_requestBuilder.addObservations(observation2);
		ReportRequest report_request = report_requestBuilder.build();
		frontend.report(report_request);
		TimeUnit.SECONDS.sleep(1);
		ReportRequest.Builder  report_requestBuilder2  = ReportRequest.newBuilder().addObservations(observation3);
		report_requestBuilder2.addObservations(observation4);
		ReportRequest report_request2 = report_requestBuilder2.build();
		frontend.report(report_request2);
	}
	
	@AfterEach
	public void tearDown() {
		ClearRequest  clear_request  = ClearRequest.newBuilder().build();
		frontend.ctrlClear(clear_request);
	}
	
		
	// tests 
	
	@Test
	public void okCarTest() {
		TrackMatchRequest trackMatchRequest = TrackMatchRequest.newBuilder()
				.setTargetType(TargetType.CAR)
				.setTargetId(VALID_CAR_SEARCH)
				.build();
		List <Observation> observations =	frontend.trackMatch(trackMatchRequest).getObservationsList();
		

		
		Observation observation = observations.get(0);
		Observation observation2 = observations.get(1);
		
		CamInfoRequest camInfoRequest = CamInfoRequest.newBuilder()
				.setCamName(observation.getCamName())
				.build();
		CamInfoResponse camInfoResponse = frontend.camInfo(camInfoRequest);
		
		CamInfoRequest camInfoRequest2 = CamInfoRequest.newBuilder()
				.setCamName(observation2.getCamName())
				.build();
		CamInfoResponse camInfoResponse2 = frontend.camInfo(camInfoRequest2);
		
		assertEquals(LATITUDE, camInfoResponse2.getLatitude(), 0.000001);
		assertEquals(LONGITUE, camInfoResponse2.getLongitude(), 0.000001);
		assertEquals(VALID_CAR,observation2.getTargetId());
		assertEquals(TargetType.CAR,observation2.getTargetType());
		assertEquals(CAM_NAME,observation2.getCamName());
		
		assertEquals(LATITUDE2, camInfoResponse.getLatitude(), 0.000001);
		assertEquals(LONGITUE2, camInfoResponse.getLongitude(), 0.000001);
		assertEquals(VALID_CAR2,observation.getTargetId());
		assertEquals(TargetType.CAR,observation.getTargetType());
		assertEquals(CAM_NAME2,observation.getCamName());
	}
	
	@Test
	public void okPersonTest() {
		TrackMatchRequest trackMatchRequest = TrackMatchRequest.newBuilder()
				.setTargetType(TargetType.PERSON)
				.setTargetId(VALID_PERSON_SEARCH)
				.build();
		List <Observation> observations =	frontend.trackMatch(trackMatchRequest).getObservationsList();
		
		Observation observation = observations.get(0);
		Observation observation2 = observations.get(1);
		
		CamInfoRequest camInfoRequest = CamInfoRequest.newBuilder()
				.setCamName(observation.getCamName())
				.build();
		CamInfoResponse camInfoResponse = frontend.camInfo(camInfoRequest);
		
		CamInfoRequest camInfoRequest2 = CamInfoRequest.newBuilder()
				.setCamName(observation2.getCamName())
				.build();
		CamInfoResponse camInfoResponse2 = frontend.camInfo(camInfoRequest2);
		
		assertEquals(LATITUDE2, camInfoResponse2.getLatitude(), 0.000001);
		assertEquals(LONGITUE2, camInfoResponse2.getLongitude(), 0.000001);
		assertEquals(VALID_PERSON2,observation2.getTargetId());
		assertEquals(TargetType.PERSON,observation2.getTargetType());
		assertEquals(CAM_NAME2,observation2.getCamName());
		
		assertEquals(LATITUDE, camInfoResponse.getLatitude(), 0.000001);
		assertEquals(LONGITUE, camInfoResponse.getLongitude(), 0.000001);
		assertEquals(VALID_PERSON,observation.getTargetId());
		assertEquals(TargetType.PERSON,observation.getTargetType());
		assertEquals(CAM_NAME,observation.getCamName());
	}
	
	@Test
	public void unkownPersonTest() {
		TrackMatchRequest trackMatchRequest = TrackMatchRequest.newBuilder()
				.setTargetType(TargetType.PERSON)
				.setTargetId(UNKNOWN_PERSON_SEARCH)
				.build();
		List <Observation> observations =	frontend.trackMatch(trackMatchRequest).getObservationsList();
		assertEquals(observations.size(),0);
	}
	
	@Test
	public void unkownCarTest() {
		TrackMatchRequest trackMatchRequest = TrackMatchRequest.newBuilder()
				.setTargetType(TargetType.CAR)
				.setTargetId(UNKNOWN_CAR_SEARCH)
				.build();
		List <Observation> observations =	frontend.trackMatch(trackMatchRequest).getObservationsList();
		assertEquals(observations.size(),0);
	}
	

	@Test
	public void invalidPersonTest() {
		TrackMatchRequest trackMatchRequest = TrackMatchRequest.newBuilder()
				.setTargetType(TargetType.PERSON)
				.setTargetId(INVALID_PERSON_SEARCH)
				.build();
		assertEquals(
				INVALID_ARGUMENT.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.trackMatch(trackMatchRequest))
			.getStatus()
			.getCode());
	}
	
	@Test
	public void invalidCarTest() {
		TrackMatchRequest trackMatchRequest = TrackMatchRequest.newBuilder()
				.setTargetType(TargetType.CAR)
				.setTargetId(INVALID_CAR_SEARCH)
				.build();
		assertEquals(
				INVALID_ARGUMENT.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.trackMatch(trackMatchRequest))
			.getStatus()
			.getCode());
	}

}