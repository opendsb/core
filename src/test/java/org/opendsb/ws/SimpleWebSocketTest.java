package org.opendsb.ws;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.glassfish.tyrus.server.Server;
import org.junit.Test;

public class SimpleWebSocketTest {

	@Test
	public void simpleTest() throws Exception {

		ExecutorService exec = Executors.newFixedThreadPool(1);

		exec.submit(() -> {
			try {
				Thread.sleep(2000);
				WebSocketContainer container = ContainerProvider.getWebSocketContainer();
				Session session = container.connectToServer(EchoClient.class,
						URI.create("ws://localhost:8025/websockets/echo"));
				session.getAsyncRemote().sendText("blabla");
			} catch (Exception e) {
			}
		});

		runServer(EchoServer.class);
	}

	private void runServer(Class<?> webSocketEndpoint) {

		Server server = new Server("localhost", 8025, "/websockets", null, webSocketEndpoint);

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
