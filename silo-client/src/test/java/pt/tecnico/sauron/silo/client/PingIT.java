package pt.tecnico.sauron.silo.client;

import pt.tecnico.sauron.silo.grpc.PingRequest;
import pt.tecnico.sauron.silo.grpc.PingResponse;

import pt.tecnico.sauron.silo.client.SiloFrontend;

import pt.tecnico.sauron.silo.client.BaseIT;

import io.grpc.StatusRuntimeException;
import static io.grpc.Status.INVALID_ARGUMENT;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PingIT extends BaseIT {
	
	// static members
	SiloFrontend frontend = new SiloFrontend("localhost", "2181", "/grpc/sauron/silo/1");
	
	
	// one-time initialization and clean-up
	@BeforeAll
	public static void oneTimeSetUp(){
		
	}

	@AfterAll
	public static void oneTimeTearDown() {
		
	}
	
	// initialization and clean-up for each test
	
	@BeforeEach
	public void setUp() {
		
	}
	
	@AfterEach
	public void tearDown() {
		
	}
		
	// tests 
	
	@Test
	public void pingOKTest() {
		PingRequest request = PingRequest.newBuilder().setInputText("friend").build();
		PingResponse response = frontend.ctrlPing(request);
		assertEquals("Hello friend!", response.getOutputText());
	}

	@Test
	public void emptyPingTest() {

		PingRequest request = PingRequest.newBuilder().setInputText("").build();
		assertEquals(
				INVALID_ARGUMENT.getCode(),
				assertThrows(StatusRuntimeException.class, () -> frontend.ctrlPing(request))
			.getStatus()
			.getCode());
	}

}
