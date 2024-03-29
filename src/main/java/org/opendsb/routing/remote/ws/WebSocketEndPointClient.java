package org.opendsb.routing.remote.ws;

import jakarta.websocket.CloseReason;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.Session;

import org.jboss.logging.Logger;

public class WebSocketEndPointClient extends Endpoint {

	private static final Logger logger = Logger.getLogger(WebSocketEndPointClient.class);

	private WebSocketPeer webSocketPeer;

	public WebSocketEndPointClient(WebSocketPeer webSocketPeer) {
		super();
		this.webSocketPeer = webSocketPeer;
	}

	@Override
	public void onOpen(Session session, EndpointConfig config) {
		webSocketPeer.setConnectionId(session.getId());
		webSocketPeer.connectionOpenned();		
		session.addMessageHandler(webSocketPeer);
	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
		logger.info("Connection to peer closed. Session id '" + session.getId() + "'. Reason code '"
				+ closeReason.getCloseCode() + "' reason phrase '" + closeReason.getReasonPhrase() + "'");
		webSocketPeer.connectionClosed(closeReason.getCloseCode().getCode(), closeReason.getReasonPhrase());
	}
	
	@Override
	public void onError(Session session, Throwable thr) {
		logger.warn("Error detected on bus remote connection to peer '" + webSocketPeer.getPeerId() + "'", thr);
	}
}
