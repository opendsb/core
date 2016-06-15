package org.opendsb.routing.remote;

import javax.websocket.CloseReason.CloseCodes;

import org.apache.log4j.Logger;
import org.opendsb.messaging.ControlMessage;
import org.opendsb.messaging.control.ControlMessageType;
import org.opendsb.messaging.control.ControlTokens;
import org.opendsb.routing.Router;

public abstract class RemoteRouterServer extends RemoteRouter {
	
	private static final Logger logger = Logger.getLogger(RemoteRouterServer.class);
	
	public RemoteRouterServer(Router localRouter) {
		super(localRouter);
	}
	
	protected abstract void createServer() throws Exception;
	
	@Override
	public void process(String connectionId, ControlMessage message) {
		if (message.getControlMessageType() == ControlMessageType.CONNECTION_REQUEST) {
			doConnectionRequest(connectionId, message);
			return;
		}
		super.process(connectionId, message);
	}

	protected void doConnectionRequest(String connectionId, ControlMessage message) {
		
		RemotePeer peer;
		
		logger.info("Receiving connection request connection id '" + connectionId + "'");
		
		try {
			if (pendingPeers.containsKey(connectionId)) {
				
				String clientId = message
						.getControlInfo(ControlTokens.CLIENT_ID);
				String transactionId = message
						.getControlInfo(ControlTokens.TRANSACTION_ID);
				peer = pendingPeers.get(connectionId);
				peer.setPeerId(clientId);
				pendingPeers.remove(connectionId);
				if (!remotePeers.containsKey(clientId)) {
					remotePeers.put(clientId, peer);
					peer.sendMessage(new ControlMessage.Builder().createConnectionReplyMessage(transactionId, id).addServerId(id).build());
				} else {
					peer.closeConnection(CloseCodes.CANNOT_ACCEPT.getCode(),
							"There is already a peer connected to this server with an id '"
									+ clientId + "'");
					logger.error("Error trying to establish a connection between client '"
							+ id
							+ "' and server '"
							+ clientId
							+ "' duplicate connection found client side. Aborting.");
				}
			} else {
				logger.error("Cannot complete connection request '" + connectionId
						+ "' pending request not found.");
			}

		} catch (Exception e) {
			logger.error("Failure processing a connection request reply.", e);
		}
	}
	
	@Override
	public void start() {
		try {
			createServer();
		} catch(Exception e) {
			logger.error("Error creating a server", e);
		}
	}
}
