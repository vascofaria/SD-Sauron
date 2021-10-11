package pt.tecnico.sauron.silo;

import pt.ulisboa.tecnico.sdis.zk.ZKNaming;
import pt.ulisboa.tecnico.sdis.zk.ZKNamingException;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class SiloServerApp {
	
	public static void main(String[] args) {
		System.out.println(SiloServerApp.class.getSimpleName());
		
		// receive and print arguments
		System.out.printf("Received %d arguments%n", args.length);
		for (int i = 0; i < args.length; i++) {
			System.out.printf("arg[%d] = %s%n", i, args[i]);
		}

		// check arguments
		if (args.length < 5) {
			System.err.println("Argument(s) missing!");
			System.err.printf("Usage: java %s zooHost zooPort host port path%n", SiloServerApp.class.getName());
			return;
		}

		final String  zooHost    = args[0];
		final String  zooPort    = args[1];
		final String  host       = args[2];
		final String  port       = args[3];
		final String  path       = args[4];
		final Integer timeToSend = Integer.parseInt(args[5]);

		String[] splitedPath = path.split("/");
		Integer replicNumber = Integer.parseInt(splitedPath[splitedPath.length-1]);

		final BindableService impl = new SiloServiceImpl(replicNumber, zooHost, zooPort, path, timeToSend);

		ZKNaming zkNaming = null;
		try {

			zkNaming = new ZKNaming(zooHost, zooPort);
			// publish
			zkNaming.rebind(path, host, port);

			// start gRPC server

			// Create a new server to listen on port
			Server server = ServerBuilder.forPort(Integer.parseInt(port)).addService(impl).build();

			// Start the server
			server.start();

			// Server threads are running in the background.
			System.out.println("Server started");

			// Do not exit the main thread. Wait until server is terminated.
			server.awaitTermination();
			
		}
		catch (IOException e) { System.err.println(e); }
		catch (InterruptedException e) { System.err.println(e); }
		catch (ZKNamingException e) { System.err.println(e); }
		finally {

			if (zkNaming != null) {
				// remove
				try { zkNaming.unbind(path, host, port); }
				catch (ZKNamingException e) { System.err.println(e); }
			}

		}
	}
}
