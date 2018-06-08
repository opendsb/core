package org.opendsb.routing.remote;

import java.lang.reflect.Type;
import java.rmi.UnexpectedException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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
	
	// Map<address, List<connectedListener>>
	private Map<String, List<CompletableFuture<Void>>> connectedListeners = new ConcurrentHashMap<>();
	
	// Map<address, List<disconnectedListener>>
	private Map<String, List<CompletableFuture<Void>>> disconnectedListeners = new ConcurrentHashMap<>();
	

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
			addPeer(peer);
			peer.sendMessage(new ControlMessage.Builder()
					.createConnectionRequestMessage("ConnectionRequest_" + UUID.randomUUID(), id)
					.addClientId(id)
					.addRoutingTableCount(localRouter.getFullSubscriptionCount())
					.build());
		} catch (Exception e) {
			logger.error("Failure establishing connection to address '" + address + "'", e);
			notifyConnection(address, true, e);
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
	protected synchronized void doConnectionReply(String connectionId, ControlMessage message) {

		logger.info("Receiving a connection request reply '" + connectionId + "'");

		String address = "";
		
		try {

			if (peers.containsKey(connectionId)) {
				RemotePeer peer = peers.get(connectionId);
				address = peer.getAddress();
				String serverId = message.getControlInfo(ControlTokens.SERVER_ID);
				Type routeTableCount = new TypeToken<Map<String, Integer>>() {}.getType();
				Gson gson = new Gson();
				Map<String, Integer> remoteRoutingTable = gson.fromJson(message.getControlInfo(ControlTokens.ROUTING_TABLE_COUNT), routeTableCount);
				peer.setPeerId(serverId);
				peer.setRemoteRoutingTableCounter(remoteRoutingTable);
				peer.activate();
				notifyConnection(address, false, null);
			} else {
				String errorMessage = "Cannot complete connection request '" + connectionId + "' pending request not found.";
				logger.error(errorMessage);
				notifyConnection(address, true, new UnexpectedException(errorMessage));
			}

		} catch (Exception e) {
			logger.error("Failure processing a connection request reply.", e);
			notifyConnection(address, true, e);
		}
	}
	
	@Override
	public void removePeer(RemotePeer peer) {
		super.removePeer(peer);
		notifyDisconnectionConnection(peer.getAddress());
	}
	
	private void notifyConnection(String address, boolean error, Throwable exception) {
		
		if(connectedListeners.containsKey(address)) {
			List<CompletableFuture<Void>> listeners = connectedListeners.get(address);
			if (error) {
				listeners.stream().forEach(cf -> cf.completeExceptionally(exception));
			} else {
				listeners.stream().forEach(cf -> cf.complete(null));
			}
			connectedListeners.remove(address);
		}
	}
	
	private void notifyDisconnectionConnection(String address) {
		if(disconnectedListeners.containsKey(address)) {
			List<CompletableFuture<Void>> listeners = disconnectedListeners.get(address);
			listeners.stream().forEach(cf -> cf.complete(null));
			disconnectedListeners.remove(address);
		}
	}
	
	public synchronized CompletableFuture<Void> whenConnected(String address) {
		
		CompletableFuture<Void> connectedFuture = new CompletableFuture<>();
		
		if(!remoteServerPaths.contains(address)) {
			connectedFuture.completeExceptionally(new IllegalArgumentException("The address: '" + address + "' is not registered in the client"));
		}
		
		RemotePeer peer = null;
		if (remoteAddressIndex.containsKey(address)) {
			peer = peers.get(remoteAddressIndex.get(address));
		}
		
		if(peer != null && peer.isConnected()) {
			 connectedFuture.complete(null);
		} else {
			List<CompletableFuture<Void>> listeners = null;
			if (connectedListeners.containsKey(address)) {
				listeners = connectedListeners.get(address);
			} else {
				listeners = new ArrayList<>();
				connectedListeners.put(address, listeners);
			}
			listeners.add(connectedFuture);
		}
		
		return connectedFuture;
	}
	
	public CompletableFuture<Void> whenDisconnected(String address) {
		
		CompletableFuture<Void> disconnectedFuture = new CompletableFuture<>();
		
		if(!remoteServerPaths.contains(address)) {
			disconnectedFuture.complete(null);
		}
		
		List<CompletableFuture<Void>> listeners = null;
		if (disconnectedListeners.containsKey(address)) {
			listeners = disconnectedListeners.get(address);
		} else {
			listeners = new ArrayList<>();
			disconnectedListeners.put(address, listeners);
		}
		listeners.add(disconnectedFuture);
		
		return disconnectedFuture;
	}
}
