package org.opendsb.routing.remote.ws;

import javax.websocket.server.ServerEndpointConfig;

import org.opendsb.routing.Router;

public class ServerConfigurator extends ServerEndpointConfig.Configurator {

	private Router router;
	private WebSocketRouterServer server;

	public ServerConfigurator(Router router, WebSocketRouterServer server) {
		super();
		this.router = router;
		this.server = server;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {

		T instance = null;

		if (endpointClass.equals(WebSocketEndPontServer.class)) {
			instance = (T) new WebSocketEndPontServer(router, server);
		}

		return instance;
	}
}
