package org.opendsb.ws.autoconnect;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.websocket.Endpoint;
import jakarta.websocket.server.ServerApplicationConfig;
import jakarta.websocket.server.ServerEndpointConfig;

import org.junit.BeforeClass;
import org.junit.Test;
import org.opendsb.routing.DefaultRouter;
import org.opendsb.routing.Router;
import org.opendsb.routing.remote.RemotePeerConnection;
import org.opendsb.ws.util.WebSocketServerHelper;

public class CloseReasonTest {


	private static String host = "localhost";
	private static int port = 8025;
	private static String path = "/webSocketPublishTest";
	private static String endPoint = "/dataMessageTest";

	
	@BeforeClass
	public static void setup() {
	}

	@Test
	public void serverStop() throws Exception {
		ExecutorService service = Executors.newFixedThreadPool(1);

		service.submit(new Client());

		WebSocketServerHelper.runTimedProgrammaticEndpointServer(host, port, path, ServerConfig.class, 2000, () -> {
		});
	}

	public static class Client implements Runnable {

		@Override
		public void run() {

			try {

				String connectionString = "ws://" + host + ":" + port + path + endPoint;
				System.out.println("Connecting to server '" + connectionString + "'");

				Router router = Router.newRouter();

				RemotePeerConnection connection = router.connectToRemoteRouter(connectionString, new HashMap<>());

				Thread.sleep(5000);

				connection.getRemotePeer().disconnect();

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static class ServerConfig implements ServerApplicationConfig {

		@Override
		public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
			Router router = new DefaultRouter();
			// server = new WebSocketRouterServer(router, endPoint, null);
			Set<ServerEndpointConfig> configs = new HashSet<>();
			// configs.add(server.getConfig());
			return configs;
		}

		@Override
		public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
			return null;
		}
	}
}
