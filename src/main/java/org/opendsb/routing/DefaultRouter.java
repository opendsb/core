package org.opendsb.routing;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.jboss.logging.Logger;
import org.opendsb.messaging.Message;
import org.opendsb.messaging.Subscription;
import org.opendsb.routing.remote.RemotePeer;
import org.opendsb.routing.remote.RemotePeerConnection;
import org.opendsb.util.SchedulingUtils;

public class DefaultRouter implements Router {

	private static final Logger logger = Logger.getLogger(DefaultRouter.class);

	
	private String routerID = "Router_" + UUID.randomUUID();
	
	private static long listenerCounter = 0;

	private ExecutorService localPoolService;

	private ExecutorService remotePoolService;

	private ScheduledExecutorService cleanUpService;

	// Map<Address, Listeners>
	protected Map<String, RouteNode> routingTable = new ConcurrentHashMap<>();
	

	// Map<ConnectionId, RemotePeer>
	protected Map<String, RemotePeer> peers = new ConcurrentHashMap<>();

	// Map<RemoteAdress, ConnectionId>
	protected Map<String, String> remoteAddressIndex = new ConcurrentHashMap<>();

	// Map<ListenerId, Listener>
	private Map<Long, Consumer<RemotePeer>> peerListeners = new ConcurrentHashMap<>();
	
	
	public DefaultRouter() {
		this(5, 5);
	}

	public DefaultRouter(int numberOFThreads) {
		this(numberOFThreads/2, numberOFThreads/2);
	}
	
	public DefaultRouter(int numberOFLocalThreads, int numberOFRemoteThreads) {
		super();
		localPoolService = SchedulingUtils.buildPool(numberOFLocalThreads, "localThread-" + routerID);
		remotePoolService = SchedulingUtils.buildPool(numberOFRemoteThreads, "remoteThread-" + routerID);
		cleanUpService = SchedulingUtils.buildScheduledPool(1, "cleanUpThread-" + routerID);
		setupCleanUpTask();
	}

	private void setupCleanUpTask() {

		Runnable cleanupTask = () -> {
			Iterator<Entry<String, RouteNode>> it = routingTable.entrySet().iterator();
			while (it.hasNext()) {
				RouteNode node = it.next().getValue();
				if (node.subscriptionCount() == 0) {
					it.remove();
				}
			}
		};

		cleanUpService.scheduleAtFixedRate(cleanupTask, 0, 60, TimeUnit.SECONDS);
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
		}
		peers.put(peer.getConnectionId(), peer);
		synchronized (peerListeners) {
			for (Consumer<RemotePeer> listener : peerListeners.values()) {
				listener.accept(peer);
			}
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
		remoteAddressIndex.remove(peer.getAddress());
	}

	public void routeMessageToPeer(Message message, RemotePeer peer) {
		remotePoolService.submit(() -> {
			try {
				peer.sendMessage(message);
			} catch (Exception e) {
				logger.error("Failed to send message to remote peer.", e);
			}
		});
	}

	@Override
	public void routeMessage(Message message, boolean remoteBroadCast) {
		logger.trace("Routing message '" + message.getType() + "' to topic '" + message.getDestination() + "'");
		RoutingTask task = new RoutingTask(this, message);
		if (remoteBroadCast) {
			// Set peers instead.
			task.setPeers(peers.values());
		}
		localPoolService.submit(task);
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

	@Override
	public long addConnectionListener(Consumer<RemotePeer> listener) {
		synchronized (peerListeners) {
			long listenerCode = listenerCounter++;
			peerListeners.put(listenerCode, listener);
			return listenerCode;
		}
	}

	@Override
	public void removeConnectionListener(long listenerCode) {
		synchronized (peerListeners) {
			peerListeners.remove(listenerCode);
		}
	}
}
