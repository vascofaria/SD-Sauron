package pt.tecnico.sauron.silo.client;

import pt.tecnico.sauron.silo.grpc.*;
import pt.tecnico.sauron.silo.client.BaseIT;

import io.grpc.StatusRuntimeException;
import static io.grpc.Status.NOT_FOUND;
import static io.grpc.Status.INVALID_ARGUMENT;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.google.protobuf.Timestamp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

public class ReportIT extends BaseIT {
	
	private static SiloFrontend siloFrontend = new SiloFrontend("localhost", "2181", "/grpc/sauron/silo/1");

	// static members
	// TODO	
	
	private static String CAM_NAME = "Tagus1";
	private static double LATITUDE = -9.303164; /* -90 a 90 */
	private static double LONGITUE = 38.737613; /*  0 a 180 */

	private static final String[]     INVALID_EYE         = {"", "  "};
	private static final String       NON_EXISTENT_EYE    = "Alamedas";
	private static final TargetType[] TARGET_TYPE         = {TargetType.PERSON, TargetType.CAR};
	private static final String[]     VALID_PERSON_IDS    = {"5638246", "6128241"};
	private static final String[]     INVALID_PERSON_IDS  = {"-12", "", "   ", "AA", "-12AA3"};
	private static final String[]     VALID_CAR_IDS       = {"AA00AA", "AA12SA"};
	private static final String[]     INVALID_CAR_IDS     = {"", " ", "-12", "AA", "AAAAAAAAaA", "D4DD12", "DDEFRT"};
	
	// one-time initialization and clean-up

	@BeforeAll
	public static void oneTimeSetUp() {
		CamJoinRequest  camJoinRequest  = CamJoinRequest.newBuilder().setCamName(CAM_NAME).setLatitude(LATITUDE).setLongitude(LONGITUE).build();
		
		CamJoinResponse camJoinResponse = siloFrontend.camJoin(camJoinRequest); 
	}

	@AfterAll
	public static void oneTimeTearDown() {
		ClearRequest clearRequest = ClearRequest.newBuilder().build();

		ClearResponse clearResponse = ClearResponse.newBuilder().build(); 
	}
	
	// initialization and clean-up for each test
	
	@BeforeEach
	public void setUp() {
		
	}
	
	@AfterEach
	public void tearDown() {
		ClearRequest clearRequest = ClearRequest.newBuilder().build();

		ClearResponse clearResponse = ClearResponse.newBuilder().build(); 
	}
		
	// tests 
	
	@Test
	public void reportASinglePersonObservationSuccessfully() {

		TrackRequest trackRequest;

		TrackResponse trackResponse;

		ReportRequest.Builder reportRequestBuilder;

		ReportRequest reportRequest;

		ReportResponse reportResponse;

		Observation observation;
		Instant time = Instant.now();
		Timestamp dateTime = Timestamp.newBuilder().setSeconds(time.getEpochSecond())
		    .setNanos(time.getNano()).build();

		reportRequestBuilder = ReportRequest.newBuilder();
		


		observation =  Observation.newBuilder().setDateTime(dateTime).setCamName(CAM_NAME).setTargetType(TargetType.PERSON).setTargetId(VALID_PERSON_IDS[0]).build();
	
		reportRequestBuilder.addObservations(observation);

		reportRequest  = reportRequestBuilder.build();

		reportResponse = siloFrontend.report(reportRequest); 

		trackRequest = TrackRequest.newBuilder().setTargetType(TargetType.PERSON).setTargetId(VALID_PERSON_IDS[0]).build();

		trackResponse = siloFrontend.track(trackRequest);

		observation = trackResponse.getObservation();

		assertEquals(observation.getTargetId(), VALID_PERSON_IDS[0]);
		assertEquals(observation.getTargetType(), TargetType.PERSON);
		assertEquals(observation.getCamName(), CAM_NAME);
	}
	
	@Test
	public void reportASingleCarObservationSuccessfully() {

		TrackRequest trackRequest;

		TrackResponse trackResponse;

		ReportRequest.Builder reportRequestBuilder;

		ReportRequest reportRequest;

		ReportResponse reportResponse;

		Observation observation;
		
		Instant time = Instant.now();
		Timestamp dateTime = Timestamp.newBuilder().setSeconds(time.getEpochSecond())
		    .setNanos(time.getNano()).build();

		reportRequestBuilder = ReportRequest.newBuilder();

		observation =  Observation.newBuilder().setDateTime(dateTime).setCamName(CAM_NAME).setTargetType(TargetType.CAR).setTargetId(VALID_CAR_IDS[0]).build();
	
		reportRequestBuilder.addObservations(observation);

		reportRequest  = reportRequestBuilder.build();

		reportResponse = siloFrontend.report(reportRequest); 

		trackRequest = TrackRequest.newBuilder().setTargetType(TargetType.CAR).setTargetId(VALID_CAR_IDS[0]).build();

		trackResponse = siloFrontend.track(trackRequest);

		observation = trackResponse.getObservation();

		assertEquals(observation.getTargetId(), VALID_CAR_IDS[0]);
		assertEquals(observation.getTargetType(), TargetType.CAR);
		assertEquals(observation.getCamName(), CAM_NAME);
	}
	
	@Test
	public void reportASinglePersonObservationWithInvalidId() {

		TrackResponse trackResponse;

		ReportRequest.Builder reportRequestBuilder;

		ReportResponse reportResponse;

		Observation observation;

		for (String personId : INVALID_PERSON_IDS) {

			reportRequestBuilder = ReportRequest.newBuilder();
			
			Instant time = Instant.now();
			Timestamp dateTime = Timestamp.newBuilder().setSeconds(time.getEpochSecond())
			    .setNanos(time.getNano()).build();

			observation =  Observation.newBuilder().setDateTime(dateTime).setCamName(CAM_NAME).setTargetType(TargetType.PERSON).setTargetId(personId).build();
		
			reportRequestBuilder.addObservations(observation);

			final ReportRequest reportRequest  = reportRequestBuilder.build();

			assertEquals(INVALID_ARGUMENT.getCode(), assertThrows(StatusRuntimeException.class, () -> siloFrontend.report(reportRequest)).getStatus().getCode());
			/*
			final TrackRequest trackRequest = TrackRequest.newBuilder().setTargetType(TargetType.PERSON).setTargetId(personId).build();

			assertEquals(NOT_FOUND.getCode(), assertThrows(StatusRuntimeException.class, () -> siloFrontend.track(trackRequest)).getStatus().getCode());
			*/
		}
	}
	
	@Test
	public void reportASingleCarObservationWithInvalidId() {

		TrackResponse trackResponse;

		ReportRequest.Builder reportRequestBuilder;

		ReportResponse reportResponse;

		Observation observation;

		for (String carId : INVALID_CAR_IDS) {

			reportRequestBuilder = ReportRequest.newBuilder();
			Instant time = Instant.now();
			Timestamp dateTime = Timestamp.newBuilder().setSeconds(time.getEpochSecond())
			    .setNanos(time.getNano()).build();

			observation =  Observation.newBuilder().setDateTime(dateTime).setCamName(CAM_NAME).setTargetType(TargetType.CAR).setTargetId(carId).build();
		
			reportRequestBuilder.addObservations(observation);

			final ReportRequest reportRequest  = reportRequestBuilder.build();

			assertEquals(INVALID_ARGUMENT.getCode(), assertThrows(StatusRuntimeException.class, () -> siloFrontend.report(reportRequest)).getStatus().getCode());
			
		}
	}

	@Test
	public void reportASingleObservationWithNonexistentEye() {
		TrackRequest trackRequest;

		TrackResponse trackResponse;

		ReportRequest.Builder reportRequestBuilder;

		ReportRequest reportRequest;

		ReportResponse reportResponse;

		Observation observation;

		reportRequestBuilder = ReportRequest.newBuilder();
		
		Instant time = Instant.now();
		Timestamp dateTime = Timestamp.newBuilder().setSeconds(time.getEpochSecond())
		    .setNanos(time.getNano()).build();

		observation =  Observation.newBuilder().setDateTime(dateTime).setCamName(NON_EXISTENT_EYE).setTargetType(TargetType.PERSON).setTargetId(VALID_PERSON_IDS[0]).build();
	
		reportRequestBuilder.addObservations(observation);

		reportRequest  = reportRequestBuilder.build();
	
		assertEquals(NOT_FOUND.getCode(), assertThrows(StatusRuntimeException.class, () -> siloFrontend.report(reportRequest)).getStatus().getCode());
	}

	@Test
	public void reportASingleObservationWithInvalidEye() {
		TrackRequest trackRequest;

		TrackResponse trackResponse;

		ReportRequest.Builder reportRequestBuilder;

		ReportResponse reportResponse;

		Observation observation;

		for (String invalidEye : INVALID_EYE) {

			final ReportRequest reportRequest;
			Instant time = Instant.now();
			Timestamp dateTime = Timestamp.newBuilder().setSeconds(time.getEpochSecond())
			    .setNanos(time.getNano()).build();

			reportRequestBuilder = ReportRequest.newBuilder();

			observation =  Observation.newBuilder().setDateTime(dateTime).setCamName(invalidEye).setTargetType(TargetType.PERSON).setTargetId(VALID_PERSON_IDS[0]).build();
	
			reportRequestBuilder.addObservations(observation);

			reportRequest  = reportRequestBuilder.build();
	
			assertEquals(INVALID_ARGUMENT.getCode(), assertThrows(StatusRuntimeException.class, () -> siloFrontend.report(reportRequest)).getStatus().getCode());
		}
	}

	@Test 
	public void reportWithoutObservations() {
		TrackRequest trackRequest;

		TrackResponse trackResponse;

		ReportRequest.Builder reportRequestBuilder;

		final ReportRequest reportRequest;

		ReportResponse reportResponse;

		Observation observation;

		reportRequestBuilder = ReportRequest.newBuilder();

		reportRequest  = reportRequestBuilder.build();
	
		assertEquals(INVALID_ARGUMENT.getCode(), assertThrows(StatusRuntimeException.class, () -> siloFrontend.report(reportRequest)).getStatus().getCode());
	}
	
	@Test
	public void reportWithoutDateTime() {

		TrackRequest trackRequest;

		TrackResponse trackResponse;

		ReportRequest.Builder reportRequestBuilder;

		ReportRequest reportRequest;

		ReportResponse reportResponse;

		Observation observation;

		reportRequestBuilder = ReportRequest.newBuilder();
		


		observation =  Observation.newBuilder().setCamName(CAM_NAME).setTargetType(TargetType.PERSON).setTargetId(VALID_PERSON_IDS[0]).build();
	
		reportRequestBuilder.addObservations(observation);

		reportRequest  = reportRequestBuilder.build();

		assertEquals(INVALID_ARGUMENT.getCode(), assertThrows(StatusRuntimeException.class, () -> siloFrontend.report(reportRequest)).getStatus().getCode());

		}

}