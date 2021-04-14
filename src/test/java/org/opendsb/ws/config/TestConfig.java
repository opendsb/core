package org.opendsb.ws.config;

import java.util.HashSet;
import java.util.Set;

import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import org.opendsb.client.BusClient;
import org.opendsb.client.DefaultBusClient;
import org.opendsb.messaging.DataMessage;
import org.opendsb.messaging.MessageType;
import org.opendsb.messaging.Subscription;
import org.opendsb.routing.LocalRouter;
import org.opendsb.routing.Router;
import org.opendsb.routing.remote.ws.WebSocketRouterServer;

public class TestConfig implements ServerApplicationConfig {

	public static Subscription subHolder = null;

	@Override
	public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
		Router router = new LocalRouter();
		BusClient client = DefaultBusClient.of(router);
		TestConfig.subHolder = client.subscribe("org/openDSB/test", m -> {
			if (m.getType() == MessageType.PUBLISH) {
				DataMessage dMsg = (DataMessage) m;
				System.out.println("Data Message received \nContent '" + dMsg.getData() + "'");
			}
		});
		WebSocketRouterServer server = new WebSocketRouterServer(router, "/mainRouter", null);
		Set<ServerEndpointConfig> configs = new HashSet<>();
		configs.add(server.getConfig());
		return configs;
	}

	@Override
	public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
		return null;
	}
}
