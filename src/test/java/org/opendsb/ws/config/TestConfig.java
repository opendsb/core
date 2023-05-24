package org.opendsb.ws.config;

import java.util.HashSet;
import java.util.Set;

import jakarta.websocket.Endpoint;
import jakarta.websocket.server.ServerApplicationConfig;
import jakarta.websocket.server.ServerEndpointConfig;

import org.opendsb.client.BusClient;
import org.opendsb.messaging.DataMessage;
import org.opendsb.messaging.MessageType;
import org.opendsb.messaging.Subscription;
import org.opendsb.routing.Router;

public class TestConfig implements ServerApplicationConfig {

	public static Subscription subHolder = null;

	@Override
	public Set<ServerEndpointConfig> getEndpointConfigs(Set<Class<? extends Endpoint>> endpointClasses) {
		
		Router router = Router.newRouter();
		BusClient client = BusClient.of(router);
		TestConfig.subHolder = client.subscribe("org/openDSB/test", m -> {
			if (m.getType() == MessageType.PUBLISH) {
				DataMessage dMsg = (DataMessage) m;
				System.out.println("Data Message received \nContent '" + dMsg.getData() + "'");
			}
		});
		
		
		// WebSocketRouterServer server = new WebSocketRouterServer(router, "/mainRouter", null);
		Set<ServerEndpointConfig> configs = new HashSet<>();
		// configs.add(server.getConfig());
		return configs;
	}

	@Override
	public Set<Class<?>> getAnnotatedEndpointClasses(Set<Class<?>> scanned) {
		return null;
	}
}
