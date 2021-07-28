package org.opendsb.routing.remote.ws;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.apache.log4j.Logger;
import org.opendsb.routing.Router;
import org.opendsb.routing.remote.RemotePeer;

public class WebSocketEndPontServer extends Endpoint {

	private static final Logger logger = Logger.getLogger(WebSocketEndPontServer.class);

	private Router router;
	
	private WebSocketRouterServer server;

	public WebSocketEndPontServer(Router router, WebSocketRouterServer server) {
		super();
		this.router = router;
		this.server = server;
	}

	@Override
	public void onOpen(Session session, EndpointConfig config) {
		WebSocketPeer webSocketPeer = new WebSocketPeer(router, session);
		webSocketPeer.connectionOpenned();
		session.addMessageHandler(webSocketPeer);
		
		server.peerConnected(webSocketPeer);
	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
		logger.info("Connection to peer closed. Session id '" + session.getId() + "'. Reason code '"
				+ closeReason.getCloseCode() + "' reason phrase '" + closeReason.getReasonPhrase() + "'");
		// Search peer using the session Id.
		RemotePeer peer = router.getPeer(session.getId());
		if (peer != null) {
			peer.connectionClosed(closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase());
		}
	}
	
	@Override
	public void onError(Session session, Throwable thr) {
		logger.warn("Error detected on bus remote server sessionId '" + session.getId() + "'", thr);
	}
}
