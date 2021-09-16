package org.opendsb.routing;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.opendsb.messaging.Message;
import org.opendsb.messaging.Subscription;
import org.opendsb.routing.remote.RemotePeer;
import org.opendsb.routing.remote.RemotePeerConnection;

public class DefaultRouter implements Router {

	
	private static final Logger logger = Logger.getLogger(DefaultRouter.class);

	
	private String routerID = "Router_" + UUID.randomUUID();
	
	private ExecutorService executorService = Executors.newFixedThreadPool(5);

	// Map<Address, Listeners>
	protected Map<String, RouteNode> routingTable = new ConcurrentHashMap<>();
	
	// Map<ConnectionId, RemotePeer>
	protected Map<String, RemotePeer> peers = new ConcurrentHashMap<>();

	// Map<RemoteAdress, ConnectionId>
	protected Map<String, String> remoteAddressIndex = new ConcurrentHashMap<>();
	
	// Map<PeerId, ConnectionId>
	protected Map<String, String> peerIdIndex = new ConcurrentHashMap<>();
	
	protected Map<Class<?>, Object> typeAdapterIdx = new ConcurrentHashMap<>();
	
	
	public DefaultRouter() {
		super();
		executorService = Executors.newFixedThreadPool(5, (r) -> {
			Thread thread = Executors.defaultThreadFactory().newThread(r);
			thread.setName("OpenDSB-" + routerID + "[" + thread.getName() + "]");
			thread.setDaemon(true);
			return thread;
		});
	}
	
	public DefaultRouter(int numberOFThreads) {
		super();
		executorService = Executors.newFixedThreadPool(numberOFThreads, (r) -> {
			Thread thread = Executors.defaultThreadFactory().newThread(r);
			thread.setName("OpenDSB-" + routerID + "[" + thread.getName() + "]");
			thread.setDaemon(true);
			return thread;
		});
	}
	
	@Override
	public String getId() {
		return routerID;
	}
	
	@Override
	public Map<String, RouteNode> getRoutingTable() {
		return routingTable;
	}
	
	@Override
	public Map<String, Integer> getFullSubscriptionCount() {
		Map<String, Integer> fullCount = new HashMap<>();
		for (String topic : routingTable.keySet()) {
			fullCount.put(topic, routingTable.get(topic).subscriptionCount());
		}
		return fullCount;
	}
	
	@Override
	public void addPeer(RemotePeer peer) throws IllegalArgumentException {
		if (peers.containsKey(peer.getConnectionId())) {
			throw new IllegalArgumentException(
					"Unable to register peer with a duplicate id '" + peer.getConnectionId() + "'");
		} else {
			peers.put(peer.getConnectionId(), peer);
		}
	}
	
	@Override
	public RemotePeer getPeer(String connectionId) {
		return peers.get(connectionId);
	}
	
	@Override
	public Stream<RemotePeer> peerStream() {
		return peers.values().stream();
	}

	@Override
	public void removePeer(RemotePeer peer) {
		peers.remove(peer.getConnectionId());
		peerIdIndex.remove(peer.getPeerId());
		remoteAddressIndex.remove(peer.getAddress());
	}

	@Override
	public void routeMessage(Message message, boolean remoteBroadCast) {
		logger.trace("Routing message '" + message.getType() + "' to topic '" + message.getDestination() + "'");
		RoutingTask task = new RoutingTask(this, message);
		if (remoteBroadCast) {
			// Set peers instead.
			task.setPeers(peers.values());
		}
		executorService.submit(task);
	}

	@Override
	public Subscription subscribe(String topic, Consumer<Message> handler, HandlerPriority priority) {

		logger.trace("subscribing to topic '" + topic + "'");

		Subscription subscription = null;
		RouteNode subNode = null;
		
		if (routingTable.containsKey(topic)) {
			subNode = routingTable.get(topic);
		} else {
			subNode = new RouteNode(topic, this);
			routingTable.put(topic, subNode);
		}
		
		subscription = subNode.subscribe(handler, priority);
		
		return subscription;
	}
	
	@Override
	public RemotePeerConnection connectToRemoteRouter(String address, Map<String, Object> opt) throws IOException {
		
		try {
			RemotePeer peer = new RemotePeer.Builder().build(this, address, opt);
			return peer.connect();
		} catch (Exception e) {
			logger.debug("Failure establishing connection to address '" + address + "'", e);
			throw new IOException("Unable to establish connection with server at '" + address + "'", e);
		}
		
	}
}
