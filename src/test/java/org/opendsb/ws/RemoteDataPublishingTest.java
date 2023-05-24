package org.opendsb.ws;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import jakarta.websocket.Endpoint;
import jakarta.websocket.server.ServerApplicationConfig;
import jakarta.websocket.server.ServerEndpointConfig;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendsb.client.BusClient;
import org.opendsb.messaging.DataMessage;
import org.opendsb.messaging.Message;
import org.opendsb.messaging.MessageType;
import org.opendsb.messaging.Subscription;
import org.opendsb.routing.Router;
import org.opendsb.routing.remote.RemotePeerConnection;
import org.opendsb.ws.util.WebSocketServerHelper;

public class RemoteDataPublishingTest {

	private static Subscription subscription;
	private static DataHandler handler = new DataHandler();

	private static String topic = "org/openDSB/remote/JSONTest";

	private static String host = "localhost";
	private static int port = 8025;
	private static String path = "/webSocketPublishTest";
	private static String endPoint = "/dataMessageTest";


	@BeforeClass
	public static void setup() {
	}

	@Test
	public void testRoutingOfAJSONAndNativeDataPackage() throws Exception {

		ExecutorService service = Executors.newFixedThreadPool(1);

		service.submit(new Client());

		WebSocketServerHelper.runUnconstrainedProgrammaticEndpointServer(host, port, path, ServerConfig.class);
	}

	@AfterClass
	public static void finish() {
		if (subscription != null) {
			subscription.cancel();
		}
	}

	public static class Client implements Runnable {

		@Override
		public void run() {

			try {

				Thread.sleep(2000);

				String connectionString = "ws://" + host + ":" + port + path + endPoint;

				System.out.println("Connecting to server '" + connectionString + "'");

				Router router = Router.newRouter();
				BusClient client = BusClient.of(router);
				
				RemotePeerConnection connection = router.connectToRemoteRouter(connectionString, new HashMap<>());
				
				connection.whenConnected().thenRun(() -> {
					Map<String, Object> data = new HashMap<>();

					data.put("someValue", 135.87);
					data.put("title", "Hello");
					data.put("message", "I've come to say hi!");

					// Native Publish
					client.publishData(topic, data);
				});

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static class DataHandler implements Consumer<Message> {

		@Override
		@SuppressWarnings("unchecked")
		public void accept(Message m) {

			if (m.getType() == MessageType.PUBLISH) {
				DataMessage dMsg = (DataMessage) m;
				Map<String, Object> data = (Map<String, Object>) dMsg.getData();
				System.out.println("Data Message received \nContent '" + data + "'");
			}
		}
	}

	public static class ServerConfig implements ServerApplicationConfig {

		@Override
		public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
			Router router = Router.newRouter();
			BusClient client = BusClient.of(router);
			subscription = client.subscribe(topic, handler);
			// WebSocketRouterServer server = new WebSocketRouterServer(router, endPoint, null);
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
