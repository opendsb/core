package org.opendsb.routing.remote;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.opendsb.messaging.ControlMessage;
import org.opendsb.messaging.Message;
import org.opendsb.messaging.MessageType;
import org.opendsb.messaging.control.ControlMessageType;
import org.opendsb.messaging.control.ControlTokens;
import org.opendsb.routing.Router;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public abstract class RemoteRouter {

	private static final Logger logger = Logger.getLogger(RemoteRouter.class);

	// Map<ConnectionId, RemotePeer>
	protected Map<String, RemotePeer> peers = new ConcurrentHashMap<>();

	// Map<RemoteAdress, ConnectionId>
	protected Map<String, String> remoteAddressIndex = new ConcurrentHashMap<>();
	// Map<PeerId, ConnectionId>
	protected Map<String, String> peerIdIndex = new ConcurrentHashMap<>();
	
	protected Router localRouter;

	protected String id;
	
	public RemoteRouter(Router localRouter) {
		super();
		this.id = localRouter.getId();
		this.localRouter = localRouter;
		this.localRouter.setRemoteRouter(this);
	}

	public String getId() {
		return id;
	}

	public void process(String connectionId, ControlMessage message) {
		logger.trace("Processing control message of type '" + message.getControlMessageType() + "'");
		if ("control".equals(message.getDestination())) {
			if (message.getControlMessageType() == ControlMessageType.UPDATE_ROUTE_COUNT) {
				RemotePeer peer = peers.get(connectionId);
				Type routeTableCount = new TypeToken<Map<String, Integer>>() {}.getType();
				Gson gson = new Gson();
				Map<String, Integer> remoteRoutingTable = gson.fromJson(message.getControlInfo(ControlTokens.ROUTING_TABLE_COUNT), routeTableCount);
				peer.setRemoteRoutingTableCounter(remoteRoutingTable);
			}
			doProcess(connectionId, message);
		}
		localRouter.routeMessage(message, false);
	}
	
	protected abstract void doProcess(String connectionId, ControlMessage message);

	public void addPeer(RemotePeer peer) throws IllegalArgumentException {
		if (peers.containsKey(peer.getConnectionId())) {
			throw new IllegalArgumentException(
					"Unable to register peer with a duplicate id '" + peer.getConnectionId() + "'");
		} else {
			peers.put(peer.getConnectionId(), peer);
		}
	}
	
	public RemotePeer getPeer(String connectionId) {
		return peers.get(connectionId);
	}

	public void removePeer(RemotePeer peer) {
		peers.remove(peer.getConnectionId());
		peerIdIndex.remove(peer.getPeerId());
		remoteAddressIndex.remove(peer.getAddress());
	}

	public abstract void start();

	public void stop() {
		cleanPeers();
	}

	protected void cleanPeers() {
		for (RemotePeer peer: peers.values()) {
			removePeer(peer);
			peer.shutdown();
		}
	}
	
	public void sendMessage(Message message) {
		String previousHop = message.getLatestHop();
		message.setLatestHop(id);
		// Send the message to all peers except the one that send the message.
		peers.values().stream().filter(peer -> !peer.getPeerId().equals(previousHop))
				.forEach(peer -> {
					// SendMesage -> Already checks if the topic is being listened in the peer else does not propagate.
					peer.sendMessage(message);
				});
	}

	public void receiveMessage(String connectionId, Message message) {
		logger.trace("Receiveing message from peer '" + connectionId + "' -> type '" + message.getType() + "'");
		if (message.getType() == MessageType.CONTROL 
				&& message instanceof ControlMessage) {
			process(connectionId, (ControlMessage) message);
			return;
		}
		sendMessage(message);
		// The above step should be asynchronous so the internal routing should
		// begin right away. (Should being the keyword. For the default
		// WebSocket implementation it is)
		localRouter.routeMessage(message, false);
	}
}
