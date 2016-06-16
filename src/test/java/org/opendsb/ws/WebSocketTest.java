package org.opendsb.ws;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.log4j.PropertyConfigurator;
import org.glassfish.tyrus.server.Server;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opendsb.client.BusClient;
import org.opendsb.client.DefaultBusClient;
import org.opendsb.routing.LocalRouter;
import org.opendsb.routing.Router;
import org.opendsb.routing.remote.RemoteRouterClient;
import org.opendsb.ws.config.TestConfig;

public class WebSocketTest {

	private static String log4JFile = "conf/log4j.properties";

	@BeforeClass
	public static void setup() {
		PropertyConfigurator.configureAndWatch(log4JFile);
	}

	@Test
	public void shouldRetryUntilServerAvailable() throws Exception {

		ExecutorService exec = Executors.newFixedThreadPool(1);

		exec.submit(() -> {
			try {
				Thread.sleep(1000);
				Router localRouter = new LocalRouter();
				BusClient client = DefaultBusClient.of(localRouter);
				RemoteRouterClient router = new RemoteRouterClient(localRouter,
						new HashSet<>(Arrays.asList("ws://localhost:8025/websockets/mainRouter")));
				router.start();
				Thread.sleep(1000);
				client.publishData("org/openDSB/test", "I have come from the client.");
			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		runServer(null);
	}

	// @Test
	public void shouldStopOnInvalidClient() throws Exception {

	}

	public void testWebSocket() throws Exception {

		ExecutorService exec = Executors.newFixedThreadPool(1);

		exec.submit(() -> {
			Session session = null;
			int i = 1;
			while (session == null) {
				try {
					Thread.sleep(0);
					WebSocketContainer container = ContainerProvider.getWebSocketContainer();
					session = container.connectToServer(SomeClient.class,
							new URI("ws://localhost:8025/websockets/echo"));
					Thread.sleep(500);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("Connection attempt " + i++ + " failed. Retrying ...");
				}
			}
		});

		runServer(EchoServer.class);
	}

	private void runServer(Class<?> webSocketEndpoint) {

		// replace class with a class object from an implementation of
		// javax.websocket.server.ServerApplicationConfig
		Server server = new Server("localhost", 8025, "/websockets", null, TestConfig.class);

		try {
			Thread.sleep(100);
			server.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("Please press a key to stop the server.");
			reader.readLine();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			server.stop();
		}
	}

}
