package org.dsb.ws.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import javax.websocket.server.ServerApplicationConfig;

import org.glassfish.tyrus.server.Server;

public class WebSocketServerHelper {

	
	public static void runUnconstrainedProgrammaticEndpointServer(String host, int port, String path, Class<? extends ServerApplicationConfig> config) {
		runTimedProgrammaticEndpointServer(host, port, path, config, -1, null);
	}
	
	public static void runTimedProgrammaticEndpointServer(String host, int port, String path, Class<? extends ServerApplicationConfig> config, long timeMills, Runnable endCode) {
		
		// replace class with a class object from an implementation of javax.websocket.server.ServerApplicationConfig
		Server server = new Server(host, port, path, null, config);

		try {
			Thread.sleep(100);
			server.start();
			if (timeMills == -1) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(
						System.in));
				System.out.println("Please press a key to stop the server.");
				reader.readLine();
			} else {
				Thread.sleep(timeMills);
				endCode.run();
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			server.stop();
		}

		
		
	}
}
