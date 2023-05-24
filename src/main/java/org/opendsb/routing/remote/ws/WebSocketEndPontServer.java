package org.opendsb.routing.remote.ws;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.CloseReason;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.server.ServerEndpointConfig;

import org.jboss.logging.Logger;
import org.opendsb.routing.Router;
import org.opendsb.routing.remote.RemotePeer;

@ApplicationScoped
@ServerEndpoint("/open-dsb/bus")
public class WebSocketEndPontServer {

	private static final Logger logger = Logger.getLogger(WebSocketEndPontServer.class);

	private Router router;
	

	@Inject
	WebSocketEndPontServer(Router router) {
		super();
		this.router = router;
	}

	@OnOpen
	public void onOpen(Session session) {
		WebSocketPeer webSocketPeer = new WebSocketPeer(router, session);
		webSocketPeer.connectionOpenned();
		session.addMessageHandler(webSocketPeer);
	}

	@OnClose
	public void onClose(Session session, CloseReason closeReason) {
		logger.debug("Connection to peer closed. Session id '" + session.getId() + "'. Reason code '"
				+ closeReason.getCloseCode() + "' reason phrase '" + closeReason.getReasonPhrase() + "'");
		// Search peer using the session Id.
		RemotePeer peer = router.getPeer(session.getId());
		if (peer != null) {
			peer.connectionClosed(closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase());
		}
	}
	
	@OnError
	public void onError(Session session, Throwable thr) {
		logger.warn("Error detected on bus remote server sessionId '" + session.getId() + "'", thr);
	}

	public static void createWebSocketEndpoint(Router localRouter, ServerContainer container, String path) throws DeploymentException {
		ServerEndpointConfig config = ServerEndpointConfig.Builder.create(WebSocketEndPontServer.class, path)
				.configurator(new ServerConfigurator(localRouter)).build();
		container.addEndpoint(config);
	}
}
