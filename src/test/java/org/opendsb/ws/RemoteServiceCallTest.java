package org.opendsb.ws;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import jakarta.websocket.Endpoint;
import jakarta.websocket.server.ServerApplicationConfig;
import jakarta.websocket.server.ServerEndpointConfig;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendsb.client.BusClient;
import org.opendsb.messaging.CallMessage;
import org.opendsb.messaging.Message;
import org.opendsb.messaging.MessageType;
import org.opendsb.messaging.ReplyMessage;
import org.opendsb.messaging.Subscription;
import org.opendsb.routing.Router;
import org.opendsb.routing.remote.RemotePeerConnection;
import org.opendsb.ws.util.WebSocketServerHelper;

public class RemoteServiceCallTest {

	private static Subscription subscription;
	private static RemoteService handler = new RemoteService();

	private static String topic = "org/openDSB/remote/testService/getInfo";

	private static String host = "localhost";
	private static int port = 8025;
	private static String path = "/webSocketPublishTest";
	private static String endPoint = "/dataMessageTest";

	@BeforeClass
	public static void setup() {
	}

	@Test
	public void remoteServiceCall() throws Exception {
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
					Object[] parametersArr = { 135, "CORRELATION_123B83DC" };

					List<Object> parameters = Arrays.asList(parametersArr);

					// Native Publish
					Future<ReplyMessage> ans = client.call(topic, parameters);
					
					try {

						ReplyVO reply = (ReplyVO) ans.get().getData();
	
						System.out.println(
								"Reply received correlation '" + reply.correlation + "', data: '" + reply.getResult() + "'.");
					} catch(Exception e) {
						e.printStackTrace();
					}
				});
				

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static class RemoteService implements Consumer<Message> {

		private BusClient client;

		public void setClient(BusClient client) {
			this.client = client;
		}

		@Override
		public void accept(Message m) {

			System.out.println("Service receiving message...");
			if (m.getType() == MessageType.CALL) {
				System.out.println("Call message identified...");
				CallMessage cMsg = (CallMessage) m;
				List<Object> parameters = cMsg.getParameters();
				String replyTopic = cMsg.getReplyTo();
				System.out.println("Retreiving call parameters...");
				if (parameters.size() == 2) {
					int param = ((Number) parameters.get(0)).intValue();
					String correlation = (String) parameters.get(1);
					System.out.println("Executing service...");
					client.publishReply(replyTopic, new ReplyVO(correlation, 10 * param));
				} else {
					client.postFailureReply(replyTopic, "One of the parameters not fonund.");
				}
			}
		}
	}

	public static class ReplyVO {

		private String correlation;
		private int result;

		public ReplyVO(String correlation, int result) {
			super();
			this.correlation = correlation;
			this.result = result;
		}

		public String getCorrelation() {
			return correlation;
		}

		public int getResult() {
			return result;
		}
	}

	public static class ServerConfig implements ServerApplicationConfig {

		@Override
		public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
			Router router = Router.newRouter();
			BusClient client = BusClient.of(router);
			handler.setClient(client);
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
