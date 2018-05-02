package org.opendsb.routing.remote.ws;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.log4j.Logger;
import org.opendsb.messaging.Message;
import org.opendsb.routing.remote.RemotePeer;
import org.opendsb.routing.remote.RemoteRouter;

public class WebSocketPeer extends RemotePeer implements MessageHandler.Whole<Message> {

	private static final Logger logger = Logger.getLogger(WebSocketPeer.class);

	private Session session = null;
	
	private String sessionCookie = "";

	public WebSocketPeer(String address, String sessionCookie, RemoteRouter router) {
		super(address, router);
		this.sessionCookie = sessionCookie;
	}

	public WebSocketPeer(RemoteRouter router, Session session) {
		super("", router);
		this.session = session;
		this.connectionId = session.getId();
	}
	

	@Override
	public void connect() throws IOException {

		WebSocketContainer container = ContainerProvider.getWebSocketContainer();

		WebSocketClient wsc = new WebSocketClient(this);

		while (session == null) {
			try {
				ClientEndpointConfig cec;
				if (sessionCookie == null || sessionCookie.isEmpty()) {
					cec = ClientEndpointConfig.Builder.create()
							.decoders(Arrays.asList(MessageDecoder.class)).encoders(Arrays.asList(MessageEncoder.class))
							.build();
				} else {
					cec = ClientEndpointConfig.Builder.create().configurator(new ClientEndpointConfig.Configurator() {
						@Override
						public void beforeRequest(Map<String, List<String>> headers) {
							super.beforeRequest(headers);
							List<String> cookieList = headers.get("Cookie");
							if (cookieList == null)
								cookieList = new ArrayList<>();
							cookieList.add(sessionCookie);
							headers.put("Cookie", cookieList);
						}
					}).decoders(Arrays.asList(MessageDecoder.class)).encoders(Arrays.asList(MessageEncoder.class))
							.build();
				}
				session = container.connectToServer(wsc, cec, address);
				connectionId = session.getId();
				router.addPendingPeer(this);
				logger.info("Connection established to '" + address.toString() + "'");
			} catch (DeploymentException e) {
				logger.debug("Error trying to setup a remote websocket connection.", e);
				throw new IOException("Unable to create a connection to the remote peer '" + address.toString() + "'.",
						e);
			} catch (IOException | IllegalStateException e) {
				logger.debug("Error trying to setup a remote websocket connection. Retrying ...");
				logger.trace("Connection failure reason", e);
			}
		}
	}

	public void onClose(Session session, CloseReason closeReason) {
		try {
			router.removePeer(this);
			peerId = "";
			if (!shutdown && closeReason.getCloseCode() != CloseCodes.CANNOT_ACCEPT) {
				connect();
			}
		} catch (IOException e) {
			logger.error("Unable to reconnect", e);
		}
	}

	@Override
	public void sendMessage(Message message) {
		session.getAsyncRemote().sendObject(message);
	}

	@Override
	public void closeConnection() {
		try {
			session.close();
		} catch (IOException e) {
			logger.debug("Error closing connection", e);
		}
	}

	@Override
	public void closeConnection(int code, String reason) {
		try {
			session.close(new CloseReason(CloseCodes.getCloseCode(code), reason));
		} catch (IOException e) {
			logger.debug("Error closing connection", e);
		}
	}

}
