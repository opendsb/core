package org.opendsb.routing.remote.ws;

import jakarta.websocket.server.ServerEndpointConfig;

import org.opendsb.routing.Router;

public class ServerConfigurator extends ServerEndpointConfig.Configurator {

	private Router router;

	public ServerConfigurator(Router router) {
		super();
		this.router = router;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {

		T instance = null;

		if (endpointClass.equals(WebSocketEndPontServer.class)) {
			instance = (T) new WebSocketEndPontServer(router);
		}

		return instance;
	}
}
