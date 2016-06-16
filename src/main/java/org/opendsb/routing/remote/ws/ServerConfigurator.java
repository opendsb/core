package org.opendsb.routing.remote.ws;

import javax.websocket.server.ServerEndpointConfig;

public class ServerConfigurator extends ServerEndpointConfig.Configurator {

	private WebSocketRouterServer router;

	public ServerConfigurator(WebSocketRouterServer router) {
		super();
		this.router = router;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {

		T instance = null;

		if (endpointClass.equals(WebSocketServer.class)) {
			instance = (T) new WebSocketServer(router);
		}

		return instance;
	}
}
