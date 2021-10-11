package pt.tecnico.sauron.silo.client;

import pt.tecnico.sauron.silo.grpc.*;
import pt.tecnico.sauron.silo.client.BaseIT;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SpotterIT extends BaseIT {
	
	private SiloFrontend SiloFrontend = new SiloFrontend("localhost", "2181", "/grpc/sauron/silo/1");
	/*
	// static members
	// TODO	
	
	private static final String[][] OBJECTS = {
		{"car","5759LL","2019-10-22T09:07:51","Tagus","38.737613","-9.303164"},
		{"car","7013LL","2019-10-04T11:02:07","Tagus","38.737613","-9.303164"},
		{"car","7013LL","2019-10-04T10:02:07","Tagus","38.737613","-9.303164"},
		{"car","7013LL","2019-10-03T08:10:20","Alameda","38.736748","-9.138908"},
		{"car","7013LL","2019-10-02T22:33:01","Tagus","38.737613","-9.303164"}
	};

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
	public void test() {
		
		
	}
	*/
}
