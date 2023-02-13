package org.opendsb.routing.remote.ws;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.websocket.ClientEndpointConfig;
import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.EncodeException;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.apache.log4j.Logger;
import org.opendsb.messaging.Message;
import org.opendsb.routing.Router;
import org.opendsb.routing.remote.RemotePeer;
import org.opendsb.routing.remote.RemotePeerConnection;

public class WebSocketPeer extends RemotePeer implements MessageHandler.Whole<String>, MessageHandler.Partial<String> {

	private static final Logger logger = Logger.getLogger(WebSocketPeer.class);

	private Session session = null;
	
	private String sessionCookie = "";
	
	private GsonCoder coder = new GsonCoder();
	
	private StringBuilder builder = new StringBuilder();
	
	protected Map<Class<?>, Object> typeAdapterIdx = new ConcurrentHashMap<>();

	
	public WebSocketPeer(Router router, String address, String sessionCookie) {
		super(router, address);
		this.sessionCookie = sessionCookie;
	}

	public WebSocketPeer(Router router, Session session) {
		super(router, "");
		this.session = session;
		this.connectionId = session.getId();
	}

	public GsonCoder getCoder() {
		return coder;
	}

	@Override
	public RemotePeerConnection doConnect() throws IOException {

		WebSocketContainer container = ContainerProvider.getWebSocketContainer();

		WebSocketEndPointClient wsc = new WebSocketEndPointClient(this);

		try {
			ClientEndpointConfig cec;
			if (sessionCookie == null || sessionCookie.isEmpty()) {
				cec = ClientEndpointConfig.Builder.create()
						.build();
			} else {
				cec = ClientEndpointConfig.Builder.create().configurator(
					new ClientEndpointConfig.Configurator() {
						@Override
						public void beforeRequest(Map<String, List<String>> headers) {
							super.beforeRequest(headers);
							List<String> cookieList = headers.get("Cookie");
							if (cookieList == null)
								cookieList = new ArrayList<>();
							cookieList.add(sessionCookie);
							headers.put("Cookie", cookieList);
						}
					}
				).build();
			}
			session = container.connectToServer(wsc, cec, URI.create(address));
			
			wireConnected = true;			
			
			logger.info("Connection established to '" + address.toString() + "' with id: '" + connectionId + "'");
			
		} catch (DeploymentException e) {
			
			logger.debug("Error trying to setup a remote websocket connection.", e);
			
			throw new IOException("Unable to create a connection to the remote peer '" + address.toString() + "'.",
					e);
		}
		
		return new RemotePeerConnection(this);
	}

	@Override
	public void closeConnection(int code, String reason) {
		try {
			session.close(new CloseReason(CloseCodes.getCloseCode(code), reason));
		} catch (IOException e) {
			logger.debug("Error closing connection", e);
		}
	}
	
	@Override
	public void doSendMessage(Message message) {
		try {
			synchronized(session) {
				String encodedMessage = coder.encode(message);
				logger.debug("Sending message: " + encodedMessage);
				session.getBasicRemote().sendText(encodedMessage);
			}
		} catch (EncodeException | IOException e) {
			logger.error("Error encoding message", e);
		} catch(Throwable e) {
			logger.error("Something went very wrong!", e);
		}
	}

	@Override
	public void onMessage(String partialMessage, boolean last) {
		logger.trace("Received a partial message");
		builder.append(partialMessage);
		if(last) {
			String fullMessage = builder.toString();
			builder = new StringBuilder();
			onMessage(fullMessage);
		}
	}

	@Override
	public void onMessage(String message) {
		try {
			logger.trace("decoding full message: " + message);
			messageReceived(coder.decode(message));
		} catch (Throwable e) {
			logger.error("Error decoding message", e);
		}
	}
}
