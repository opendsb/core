package org.opendsb.routing.remote.ws;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.apache.log4j.Logger;
import org.opendsb.routing.remote.RemotePeer;
import org.opendsb.routing.remote.RemoteRouterServer;

public class WebSocketServer extends Endpoint {
	
	private static final Logger logger = Logger.getLogger(WebSocketServer.class);
	
	private RemoteRouterServer router;

	public WebSocketServer(RemoteRouterServer router) {
		super();
		this.router = router;
	}

	@Override
	public void onOpen(Session session, EndpointConfig config) {
		WebSocketPeer webSocketPeer = new WebSocketPeer(router, session);
		session.addMessageHandler(webSocketPeer);
		router.addPendingPeer(webSocketPeer);
	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
		logger.info("Connection to peer closed. Session id '" + session.getId()
				+ "'. Reason code '"+ closeReason.getCloseCode() + "' reason phrase '" + closeReason.getReasonPhrase() + "'");
		// Search peer using the session Id.
		RemotePeer peer = router.findPeerByConnectionId(session.getId());
		if (peer != null) {
			router.removePeer(peer);
		}
	}
}
