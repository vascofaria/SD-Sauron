package pt.tecnico.sauron.silo.client;

import pt.tecnico.sauron.silo.grpc.*;
import pt.tecnico.sauron.silo.client.BaseIT;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static io.grpc.Status.INVALID_ARGUMENT;
import static io.grpc.Status.NOT_FOUND;

import com.google.protobuf.Timestamp;

import io.grpc.StatusRuntimeException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class TrackIT extends BaseIT {
	
	private final SiloFrontend frontend = new SiloFrontend("localhost", "2181", "/grpc/sauron/silo/1");


	private static String CAM_NAME = "Tagus";
	private static double LATITUDE = -9.303164; 
	private static double LONGITUE = 38.737613; 
	
	
	private static final String VALID_CAR = "DD4341";
	private static final String VALID_PERSON = "12345678";
	private static final String UKNOWN_CAR = "DD33DD";
	private static final String UNKOWN_PERSON = "98989898";
	private static final String INVALID_CAR_ID = "D2D2D2";
	private static final String INVALID_PERSON_ID = "123222D";



	// one-time initialization and clean-up
	@BeforeAll
	public static void oneTimeSetUp(){


		
	}
	

	@AfterAll
	public static void oneTimeTearDown() {

		
	}
	
	@BeforeEach
	public void setUp() {
		ClearRequest  clear_request  = ClearRequest.newBuilder().build();
		frontend.ctrlClear(clear_request);
		CamJoinRequest  request = CamJoinRequest.newBuilder()
												.setCamName(CAM_NAME)
												.setLatitude(LATITUDE)
												.setLongitude(LONGITUE)
												.build();
		frontend.camJoin(request);
		
		
		
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

		ReportRequest.Builder  report_requestBuilder  = ReportRequest.newBuilder().addObservations(observation1);
		report_requestBuilder.addObservations(observation2);
		ReportRequest report_request = report_requestBuilder.build();
		frontend.report(report_request);
	}
	
	@AfterEach
	public void tearDown() {
		ClearRequest  clear_request  = ClearRequest.newBuilder().build();
		frontend.ctrlClear(clear_request);
	}
	
		
	// tests 
	
	@Test
	public void okCarTest() {
		TrackRequest trackRequest = TrackRequest.newBuilder()
				.setTargetType(TargetType.CAR)
				.setTargetId(VALID_CAR)
				.build();
		Observation observation =	frontend.track(trackRequest).getObservation();
		CamInfoRequest camInfoRequest = CamInfoRequest.newBuilder()
				.setCamName(observation.getCamName())
				.build();
		CamInfoResponse camInfoResponse = frontend.camInfo(camInfoRequest);
		assertEquals(LATITUDE, camInfoResponse.getLatitude(), 0.000001);
		assertEquals(LONGITUE, camInfoResponse.getLongitude(), 0.000001);
		assertEquals(VALID_CAR,observation.getTargetId());
		assertEquals(TargetType.CAR,observation.getTargetType());
		assertEquals(CAM_NAME,observation.getCamName());
	}
	
	@Test
	public void okPersonTest() {
		TrackRequest trackRequest = TrackRequest.newBuilder()
				.setTargetType(TargetType.PERSON)
				.setTargetId(VALID_PERSON)
				.build();
		Observation observation =	frontend.track(trackRequest).getObservation();
		CamInfoRequest camInfoRequest = CamInfoRequest.newBuilder()
				.setCamName(observation.getCamName())
				.build();
		CamInfoResponse camInfoResponse = frontend.camInfo(camInfoRequest);
		assertEquals(LATITUDE, camInfoResponse.getLatitude(), 0.000001);
		assertEquals(LONGITUE, camInfoResponse.getLongitude(), 0.000001);
		assertEquals(VALID_PERSON,observation.getTargetId());
		assertEquals(TargetType.PERSON,observation.getTargetType());
		assertEquals(CAM_NAME,observation.getCamName());
	}
	
	@Test
	public void unkownPersonTest() {
		TrackRequest trackRequest = TrackRequest.newBuilder()
				.setTargetType(TargetType.PERSON)
				.setTargetId(UNKOWN_PERSON)
				.build();
		assertEquals(
				NOT_FOUND.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.track(trackRequest))
			.getStatus()
			.getCode());
	}
	
	@Test
	public void unkownCarTest() {
		TrackRequest trackRequest = TrackRequest.newBuilder()
				.setTargetType(TargetType.CAR)
				.setTargetId(UKNOWN_CAR)
				.build();
		assertEquals(
				NOT_FOUND.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.track(trackRequest))
			.getStatus()
			.getCode());
	}
	
	@Test
	public void invalidCarTest() {
		TrackRequest trackRequest = TrackRequest.newBuilder()
				.setTargetType(TargetType.CAR)
				.setTargetId(INVALID_CAR_ID)
				.build();
		assertEquals(
				NOT_FOUND.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.track(trackRequest))
			.getStatus()
			.getCode());
	}
	
	@Test
	public void invalidPersonTest() {
		TrackRequest trackRequest = TrackRequest.newBuilder()
				.setTargetType(TargetType.CAR)
				.setTargetId(INVALID_PERSON_ID)
				.build();
		assertEquals(
				NOT_FOUND.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.track(trackRequest))
			.getStatus()
			.getCode());
	}	

}
