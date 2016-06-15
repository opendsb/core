package org.opendsb.routing.remote;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.opendsb.messaging.ControlMessage;
import org.opendsb.messaging.Message;
import org.opendsb.messaging.MessageType;
import org.opendsb.routing.Router;

public abstract class RemoteRouter {

	private static final Logger logger = Logger.getLogger(RemoteRouter.class);
	
	protected Map<String, RemotePeer> remotePeers = new HashMap<>();
	
	protected Map<String, RemotePeer> pendingPeers = new HashMap<>();
	
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
		logger.error("Control message of the type '" + message.getControlMessageType() + "' could not be processed.");
	}
	
	public void addPendingPeer(RemotePeer peer) throws IllegalArgumentException {
		if(pendingPeers.containsKey(peer.getConnectionId())) {
			throw new IllegalArgumentException("Unable to register peer with a duplicate id '" + peer.getConnectionId() + "'");
		} else {
			pendingPeers.put(peer.getConnectionId(), peer);
		}
	}
	
	public void removePeer(RemotePeer peer) {
		String connectionId = peer.getConnectionId();
		String peerId = peer.getPeerId();
		pendingPeers.remove(connectionId);
		remotePeers.remove(peerId);
	}
		
	public RemotePeer findPeerByConnectionId(String connectionId) {
		RemotePeer peer = null;
		
		peer = pendingPeers.get(connectionId);
		
		if (peer == null) {
			Optional<RemotePeer> possiblePeer = remotePeers.values().stream()
					.filter(p -> p.getConnectionId().equals(connectionId))
					.findFirst();
			if (possiblePeer.isPresent()) {
				peer = possiblePeer.get();
			}
		}
		
		return peer;
	}
	
	public abstract void start();
	
	public void stop() {
		cleanPeers(pendingPeers);
		cleanPeers(remotePeers);
	}
	
	protected void cleanPeers(Map<String, RemotePeer> peerMapping) {
		Iterator<Entry<String, RemotePeer>> it = peerMapping.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, RemotePeer> entry = it.next();
			entry.getValue().shutdown();
			it.remove();
		}
	}
	
	public void sendMessage(Message message) {
		String previousHop = message.getLatestHop();
		message.setLatestHop(id);
		// Send the message to all peers except the one that send the message.
		remotePeers.entrySet().stream().filter(e -> !e.getKey().equals(previousHop)).forEach(e -> e.getValue().sendMessage(message));
	}
		
	public void receiveMessage(String connectionId, Message message) {
		if(message.getType() == MessageType.CONTROL && message instanceof ControlMessage) {
			process(connectionId, (ControlMessage)message);
			return;
		}
		sendMessage(message);
		// The above step should be asynchronous so the internal routing should
		// begin right away. (Should being the keyword. For the default
		// WebSocket implementation it is)
		localRouter.routeMessage(message, false);
	}
}
