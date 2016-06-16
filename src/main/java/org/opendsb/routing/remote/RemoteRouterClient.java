package org.opendsb.routing.remote;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.opendsb.messaging.ControlMessage;
import org.opendsb.messaging.control.ControlMessageType;
import org.opendsb.messaging.control.ControlTokens;
import org.opendsb.routing.Router;

public class RemoteRouterClient extends RemoteRouter {

	private static final Logger logger = Logger.getLogger(RemoteRouterClient.class);

	private Set<String> remoteServerPaths = new HashSet<>();

	public RemoteRouterClient(Router localRouter, Set<String> remoteServerPaths) {
		super(localRouter);
		this.remoteServerPaths = remoteServerPaths;
	}

	@Override
	public void start() {
		remoteServerPaths.stream().forEach(path -> connectToRemoteAddress(path));
	}

	private void connectToRemoteAddress(String address) {
		try {
			RemotePeer peer = new RemotePeer.Builder().build(address, this);
			peer.connect();
			pendingPeers.put(peer.getConnectionId(), peer);
			peer.sendMessage(new ControlMessage.Builder()
					.createConnectionRequestMessage("ConnectionRequest_" + UUID.randomUUID(), id).addClientId(id)
					.build());
		} catch (Exception e) {
			logger.error("Failure establishing connection to address '" + address + "'", e);
		}
	}

	@Override
	public void process(String connectionId, ControlMessage message) {
		if (message.getControlMessageType() == ControlMessageType.CONNECTION_REPLY) {
			doConnectionReply(connectionId, message);
			return;
		}
		super.process(connectionId, message);
	}

	// FIXME: 1003 code connected to WebSocket. Make an enum that encapsulates
	// that regardless of transport.
	protected void doConnectionReply(String connectionId, ControlMessage message) {

		logger.info("Receiving a connection request reply '" + connectionId + "'");

		try {

			if (pendingPeers.containsKey(connectionId)) {
				RemotePeer peer = pendingPeers.get(connectionId);
				String serverId = message.getControlInfo(ControlTokens.SERVER_ID);
				peer.setPeerId(serverId);
				pendingPeers.remove(connectionId);
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
