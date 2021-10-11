package pt.tecnico.sauron.silo.client;

import pt.tecnico.sauron.silo.grpc.PingRequest;
import pt.tecnico.sauron.silo.grpc.PingResponse;

import pt.tecnico.sauron.silo.client.SiloFrontend;

public class SiloClientApp {

	/**
	 * Set flag to true to print debug messages. The flag can be set using the
	 * -Ddebug command line option.
	 */
	private static final boolean DEBUG_FLAG = (System.getProperty("debug") != null);
	
	/** Helper method to print debug messages. */
	private static void debug(String debugMessage) {
		if (DEBUG_FLAG)
			System.err.println(debugMessage);
	}

	public static void main(String[] args) {
		System.out.println(SiloClientApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// check arguments
		if (args.length < 3) {
			System.out.println("Argument(s) missing!");
			System.out.printf("Usage: java %s zooHost zooPort path%n", SiloClientApp.class.getName());
			return;
		}

		final String zooHost = args[0];
		final String zooPort = args[1];
		final String path    = args[2];

		SiloFrontend frontend = new SiloFrontend(zooHost, zooPort, path);

		PingRequest request = PingRequest.newBuilder().setInputText("friend").build();
		PingResponse response = frontend.ctrlPing(request);
		System.out.println(response);

		/*try {

			PingRequest request = PingRequest.newBuilder().setInputText("").build();
			PingResponse response = frontend.ctrlPing(request);

		} catch (StatusRuntimeException e) {
			System.out.println("Caught exception with description: " + e.getStatus().getDescription());
		}*/		

	}
	
}
