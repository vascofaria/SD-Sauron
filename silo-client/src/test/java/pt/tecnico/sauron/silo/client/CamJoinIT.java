package pt.tecnico.sauron.silo.client;

import pt.tecnico.sauron.silo.grpc.CamJoinRequest;
import pt.tecnico.sauron.silo.grpc.CamJoinResponse;

import pt.tecnico.sauron.silo.grpc.CamInfoRequest;
import pt.tecnico.sauron.silo.grpc.CamInfoResponse;

import pt.tecnico.sauron.silo.grpc.ClearRequest;
import pt.tecnico.sauron.silo.grpc.ClearResponse;

import pt.tecnico.sauron.silo.client.BaseIT;

import io.grpc.StatusRuntimeException;
import static io.grpc.Status.INVALID_ARGUMENT;
import static io.grpc.Status.ALREADY_EXISTS;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CamJoinIT extends BaseIT {
	
	private final SiloFrontend frontend = new SiloFrontend("localhost", "2181", "/grpc/sauron/silo/1");
	
	// static members
	private static String CAM_NAME = "Tagus";

	private static double LATITUDE = -9.303164; /*  -90  a  90 */
	private static double LONGITUE = 38.737613; /* -180 a 180  */

	private static double INVALID_LATITUDE  =  100.245465;
	private static double INVALID_LONGITUDE = -250.245465;
	
	// one-time initialization and clean-up
	@BeforeAll
	public static void oneTimeSetUp() {
		
	}

	@AfterAll
	public static void oneTimeTearDown() {
		
	}
	
	// initialization and clean-up for each test
	
	@BeforeEach
	public void setUp() {
		ClearRequest  clearRequest  = ClearRequest.newBuilder().build();
		ClearResponse clearResponse = frontend.ctrlClear(clearRequest);
	}
	
	@AfterEach
	public void tearDown() {
		
	}
		
	// tests 

	@Test
	public void camJoinOKTest() {
		CamJoinRequest  camJoinRequest = CamJoinRequest.newBuilder()
												.setCamName(CAM_NAME)
												.setLatitude(LATITUDE)
												.setLongitude(LONGITUE)
												.build();
		CamJoinResponse camJoinResponse = frontend.camJoin(camJoinRequest);
		
		CamInfoRequest  camInfoRequest = CamInfoRequest.newBuilder()
												.setCamName(CAM_NAME)
												.build();
		CamInfoResponse camInfoResponse = frontend.camInfo(camInfoRequest);
		
		assertEquals(LATITUDE, camInfoResponse.getLatitude(), 0.000001);
		assertEquals(LONGITUE, camInfoResponse.getLongitude(), 0.000001);
	}

	@Test
	public void invalidCoordsTest() {
		CamJoinRequest  camJoinRequest = CamJoinRequest.newBuilder()
												.setCamName(CAM_NAME)
												.setLatitude(INVALID_LATITUDE)
												.setLongitude(INVALID_LONGITUDE)
												.build();

		assertEquals(
				INVALID_ARGUMENT.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.camJoin(camJoinRequest))
			.getStatus()
			.getCode());
	}

	@Test
	public void dupCamTest() {

		CamJoinRequest  camJoinRequest = CamJoinRequest.newBuilder()
												.setCamName(CAM_NAME)
												.setLatitude(LATITUDE)
												.setLongitude(LONGITUE)
												.build();
		CamJoinResponse camJoinResponse = frontend.camJoin(camJoinRequest);

		CamJoinRequest camJoinRequestDup = CamJoinRequest.newBuilder()
								.setCamName(CAM_NAME)
								.setLatitude(LATITUDE + 1)
								.setLongitude(LONGITUE + 1)
								.build();
		CamInfoRequest  camInfoRequest = CamInfoRequest.newBuilder()
				.setCamName(CAM_NAME)
				.build();
		
		CamInfoResponse camInfoResponse = frontend.camInfo(camInfoRequest);
		
		assertEquals(LATITUDE, camInfoResponse.getLatitude(), 0.000001);
		assertEquals(LONGITUE, camInfoResponse.getLongitude(), 0.000001);
	}
}
