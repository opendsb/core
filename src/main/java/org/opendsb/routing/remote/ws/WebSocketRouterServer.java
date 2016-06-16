package org.opendsb.routing.remote.ws;

import java.util.Arrays;

import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import org.opendsb.routing.Router;
import org.opendsb.routing.remote.RemoteRouterServer;

public class WebSocketRouterServer extends RemoteRouterServer {

	private String path;
	private ServerContainer container;

	private ServerEndpointConfig config;

	public WebSocketRouterServer(Router localRouter, String path, ServerContainer container) {
		super(localRouter);
		this.path = path;
		this.container = container;
		this.config = ServerEndpointConfig.Builder.create(WebSocketServer.class, path)
				.decoders(Arrays.asList(MessageDecoder.class)).encoders(Arrays.asList(MessageEncoder.class))
				.configurator(new ServerConfigurator(this)).build();
	}

	public String getPath() {
		return path;
	}

	public ServerEndpointConfig getConfig() {
		return config;
	}

	@Override
	protected void createServer() throws Exception {
		if (container != null) {
			container.addEndpoint(config);
		} else {
			throw new IllegalStateException("Atempt to start a server with ServerContainer null.");
		}
	}

}
