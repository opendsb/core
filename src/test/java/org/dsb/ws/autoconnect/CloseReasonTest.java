package org.dsb.ws.autoconnect;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import org.apache.log4j.PropertyConfigurator;
import org.dsb.ws.util.WebSocketServerHelper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendsb.routing.LocalRouter;
import org.opendsb.routing.Router;
import org.opendsb.routing.remote.RemoteRouter;
import org.opendsb.routing.remote.RemoteRouterClient;
import org.opendsb.routing.remote.ws.WebSocketRouterServer;

public class CloseReasonTest {

	private static WebSocketRouterServer server;
	
	private static String host = "localhost";
	private static int port = 8025;
	private static String path = "/webSocketPublishTest";
	private static String endPoint = "/dataMessageTest";
	
	private static String log4JFile = "conf/log4j.properties";
	
	@BeforeClass
	public static void setup() {
		PropertyConfigurator.configureAndWatch(log4JFile);
	}
	
	@Test
	public void serverStop() throws Exception {
		ExecutorService service = Executors.newFixedThreadPool(1);
		
		service.submit(new Client());
		
		WebSocketServerHelper.runTimedProgrammaticEndpointServer(host, port, path, ServerConfig.class, 2000, () -> {
			if (server != null) {
//				server.stop();
			}
		});
	}

	
	public static class Client implements Runnable {

		@Override
		public void run() {
			
			try {
				
				String connectionString = "ws://" + host + ":" + port + path + endPoint;
				System.out.println("Connecting to server '" + connectionString + "'");
				
				Router router = new LocalRouter();
				
				RemoteRouter remote = new RemoteRouterClient(router, new HashSet<String>(Arrays.asList(connectionString)));

				remote.start();
				
				Thread.sleep(5000);
				
				remote.stop();
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	public static class ServerConfig implements ServerApplicationConfig {

		@Override
		public Set<ServerEndpointConfig> getEndpointConfigs(
				Set<Class<? extends Endpoint>> endpointClasses) {
			Router router = new LocalRouter();
			server = new WebSocketRouterServer(router, endPoint, null);
			Set<ServerEndpointConfig> configs = new HashSet<>();
			configs.add(server.getConfig());
			return configs;
		}

		@Override
		public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
			return null;
		}	
	}
}
