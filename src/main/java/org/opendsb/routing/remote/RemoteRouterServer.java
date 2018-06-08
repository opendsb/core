package org.opendsb.routing.remote;

import java.lang.reflect.Type;
import java.util.Map;

import org.apache.log4j.Logger;
import org.opendsb.messaging.ControlMessage;
import org.opendsb.messaging.control.ControlMessageType;
import org.opendsb.messaging.control.ControlTokens;
import org.opendsb.routing.Router;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public abstract class RemoteRouterServer extends RemoteRouter {

	private static final Logger logger = Logger.getLogger(RemoteRouterServer.class);

	public RemoteRouterServer(Router localRouter) {
		super(localRouter);
	}

	protected abstract void createServer() throws Exception;

	@Override
	public void doProcess(String connectionId, ControlMessage message) {
		if (message.getControlMessageType() == ControlMessageType.CONNECTION_REQUEST) {
			doConnectionRequest(connectionId, message);
			return;
		}
	}

	protected void doConnectionRequest(String connectionId, ControlMessage message) {

		RemotePeer peer;

		logger.debug("Receiving connection request connection id '" + connectionId + "'");

		try {
			if (peers.containsKey(connectionId)) {
				String clientId = message.getControlInfo(ControlTokens.CLIENT_ID);
				String transactionId = message.getControlInfo(ControlTokens.TRANSACTION_ID);
				Type routeTableCount = new TypeToken<Map<String, Integer>>() {}.getType();
				Gson gson = new Gson();
				Map<String, Integer> remoteRoutingTable = gson.fromJson(message.getControlInfo(ControlTokens.ROUTING_TABLE_COUNT), routeTableCount);
				peer = peers.get(connectionId);
				peer.setPeerId(clientId);
				peer.setRemoteRoutingTableCounter(remoteRoutingTable);
				peer.sendMessage(new ControlMessage.Builder()
						.createConnectionReplyMessage(transactionId, id)
						.addRoutingTableCount(localRouter.getFullSubscriptionCount())
						.addServerId(id)
						.build());
				peer.activate();
			} else {
				logger.error("Cannot complete connection request '" + connectionId + "' pending request not found.");
			}

		} catch (Exception e) {
			logger.error("Failure processing a connection request reply.", e);
		}
	}

	@Override
	public void start() {
		try {
			createServer();
		} catch (Exception e) {
			logger.error("Error creating a server", e);
		}
	}
}
