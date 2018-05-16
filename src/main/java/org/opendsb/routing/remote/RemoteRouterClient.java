package org.opendsb.routing.remote;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.opendsb.messaging.ControlMessage;
import org.opendsb.messaging.control.ControlMessageType;
import org.opendsb.messaging.control.ControlTokens;
import org.opendsb.routing.Router;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class RemoteRouterClient extends RemoteRouter {

	private static final Logger logger = Logger.getLogger(RemoteRouterClient.class);

	private Set<String> remoteServerPaths = new HashSet<>();
	
	private String sessionCookie = "";

	public RemoteRouterClient(Router localRouter, Set<String> remoteServerPaths) {
		super(localRouter);
		this.remoteServerPaths = remoteServerPaths;
	}

	
	public RemoteRouterClient(Router localRouter, Set<String> remoteServerPaths, String sessionCookie) {
		super(localRouter);
		this.remoteServerPaths = remoteServerPaths;
		this.sessionCookie = sessionCookie;
	}

	@Override
	public void start() {
		remoteServerPaths.stream().forEach(path -> connectToRemoteAddress(path));
	}

	private void connectToRemoteAddress(String address) {
		try {
			RemotePeer peer = new RemotePeer.Builder().build(address, this, sessionCookie);
			peer.connect();
			addPendingPeer(peer);
			peer.sendMessage(new ControlMessage.Builder()
					.createConnectionRequestMessage("ConnectionRequest_" + UUID.randomUUID(), id)
					.addClientId(id)
					.addRoutingTableCount(localRouter.getFullSubscriptionCount())
					.build());
		} catch (Exception e) {
			logger.error("Failure establishing connection to address '" + address + "'", e);
		}
	}

	@Override
	public void doProcess(String connectionId, ControlMessage message) {
		if (message.getControlMessageType() == ControlMessageType.CONNECTION_REPLY) {
			doConnectionReply(connectionId, message);
			return;
		}
	}

	// FIXME: 1003 code connected to WebSocket. Make an enum that encapsulates
	// that regardless of transport.
	protected void doConnectionReply(String connectionId, ControlMessage message) {

		logger.info("Receiving a connection request reply '" + connectionId + "'");

		try {

			if (pendingPeers.containsKey(connectionId)) {
				RemotePeer peer = pendingPeers.remove(connectionId);
				String serverId = message.getControlInfo(ControlTokens.SERVER_ID);
				Type routeTableCount = new TypeToken<Map<String, Integer>>() {}.getType();
				Gson gson = new Gson();
				Map<String, Integer> remoteRoutingTable = gson.fromJson(message.getControlInfo(ControlTokens.ROUTING_TABLE_COUNT), routeTableCount);
				peer.setPeerId(serverId);
				peer.setRemoteRoutingTableCounter(remoteRoutingTable);
				if (!remotePeers.containsKey(serverId)) {
					remotePeers.put(serverId, peer);
				} else {
					String reason = "Error trying to establish a connection between client '" + id + "' and server '"
							+ serverId + "' duplicate connection found client side. Aborting.";
					peer.closeConnection(1003, reason);
					logger.error(reason);
				}
			} else {
				logger.error("Cannot complete connection request '" + connectionId + "' pending request not found.");
			}

		} catch (Exception e) {
			logger.error("Failure processing a connection request reply.", e);
		}
	}
}
