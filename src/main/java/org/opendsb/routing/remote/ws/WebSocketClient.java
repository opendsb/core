package org.opendsb.routing.remote.ws;

import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

import org.apache.log4j.Logger;


public class WebSocketClient extends Endpoint {
	
	private static final Logger logger = Logger.getLogger(WebSocketClient.class);
	
	private WebSocketPeer webSocketPeer;

	public WebSocketClient(WebSocketPeer webSocketPeer) {
		super();
		this.webSocketPeer = webSocketPeer;
	}

	@Override
	public void onOpen(Session session, EndpointConfig config) {
		session.addMessageHandler(webSocketPeer);
	}

	@Override
	public void onClose(Session session, CloseReason closeReason) {
		logger.info("Connection to peer closed. Session id '" + session.getId()
				+ "'. Reason code '"+ closeReason.getCloseCode() + "' reason phrase '" + closeReason.getReasonPhrase() + "'");
		webSocketPeer.onClose(session, closeReason);
	}
}
