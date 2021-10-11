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
import static io.grpc.Status.NOT_FOUND;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CamInfoIT extends BaseIT {
	
	private final SiloFrontend frontend = new SiloFrontend("localhost", "2181", "/grpc/sauron/silo/1");
	
	// static members
	private static String CAM_NAME = "Tagus";

	private static double LATITUDE = -9.303164; /* -90 a 90   */
	private static double LONGITUE = 38.737613; /* -180 - 180 */
	
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
	public void camInfoOKTest() {
		CamJoinRequest  camJoinRequest  = CamJoinRequest.newBuilder()
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
	public void camNotFoundTest() {
		CamInfoRequest  camInfoRequest = CamInfoRequest.newBuilder()
												.setCamName(CAM_NAME)
												.build();

		assertEquals(
				NOT_FOUND.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.camInfo(camInfoRequest))
			.getStatus()
			.getCode());
	}

}
