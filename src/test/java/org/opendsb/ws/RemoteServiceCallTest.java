package org.opendsb.ws;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import org.apache.log4j.PropertyConfigurator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendsb.client.BusClient;
import org.opendsb.client.DefaultBusClient;
import org.opendsb.messaging.CallMessage;
import org.opendsb.messaging.Message;
import org.opendsb.messaging.MessageType;
import org.opendsb.messaging.ReplyMessage;
import org.opendsb.messaging.Subscription;
import org.opendsb.routing.LocalRouter;
import org.opendsb.routing.Router;
import org.opendsb.routing.remote.RemoteRouter;
import org.opendsb.routing.remote.RemoteRouterClient;
import org.opendsb.routing.remote.ws.WebSocketRouterServer;
import org.opendsb.ws.util.WebSocketServerHelper;

public class RemoteServiceCallTest {

	private static Subscription subscription;
	private static RemoteService handler = new RemoteService();

	private static String topic = "/org/openDSB/remote/testService/getInfo";

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

				Router router = new LocalRouter();
				BusClient client = DefaultBusClient.of(router);

				RemoteRouter remote = new RemoteRouterClient(router,
						new HashSet<String>(Arrays.asList(connectionString)));

				remote.start();

				Thread.sleep(200);

				Map<String, Object> parameters = new HashMap<>();

				parameters.put("param", 135);
				parameters.put("correlation", "CORRELATION_123B83DC");

				// Native Publish
				Future<ReplyMessage> ans = client.call(topic, parameters, () -> {});

				ReplyVO reply = (ReplyVO) ans.get().getData();

				System.out.println(
						"Reply received correlation '" + reply.correlation + "', data: '" + reply.getResult() + "'.");

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
				Map<String, Object> parameters = cMsg.getParameters();
				String replyTopic = cMsg.getReplyTo();
				System.out.println("Retreiving call parameters...");
				if (parameters.containsKey("correlation") && parameters.containsKey("param")) {
					int param = ((Number) parameters.get("param")).intValue();
					String correlation = (String) parameters.get("correlation");
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
			Router router = new LocalRouter();
			BusClient client = DefaultBusClient.of(router);
			handler.setClient(client);
			subscription = client.subscribe(topic, handler);
			WebSocketRouterServer server = new WebSocketRouterServer(router, endPoint, null);
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
