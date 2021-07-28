package org.opendsb.routing.remote.ws;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpointConfig;

import org.opendsb.routing.Router;
import org.opendsb.routing.remote.RemotePeer;
import org.opendsb.routing.remote.RouterServer;

public class WebSocketRouterServer extends RouterServer {

	private String path;
	private ServerContainer container;

	private ServerEndpointConfig config;
	
	private Map<Long, Consumer<RemotePeer>> listeners = new HashMap<>();
	
	private static long listenerCounter = 0;

	public WebSocketRouterServer(Router router, String path, ServerContainer container) {
		super(router);
		this.path = path;
		this.container = container;
		this.config = ServerEndpointConfig.Builder.create(WebSocketEndPontServer.class, path)
				.configurator(new ServerConfigurator(router, this)).build();
	}

	public String getPath() {
		return path;
	}

	public ServerEndpointConfig getConfig() {
		return config;
	}
	
	public long addConnectionListener(Consumer<RemotePeer> listener) {
		synchronized (listeners) {
			long listenerCode = listenerCounter++;
			listeners.put(listenerCode, listener);
			return listenerCode;
		}
	}
	
	public void peerConnected(RemotePeer remotePeer) {
		synchronized (listeners) {
			for (Consumer<RemotePeer> listener : listeners.values()) {
				listener.accept(remotePeer);
			}
		}
	}
	

	@Override
	public void start() throws IOException {
		if (container != null) {
			try {
				container.addEndpoint(config);
			} catch(Exception e) {
				throw new IOException("Unable to start Bus WebSocket server.", e);
			}
		} else {
			throw new IOException("Atempt to start a server with ServerContainer null.");
		}
	}

	
}
