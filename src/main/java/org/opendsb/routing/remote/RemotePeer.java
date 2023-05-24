package org.opendsb.routing.remote;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.logging.Logger;
import org.opendsb.messaging.CallMessage;
import org.opendsb.messaging.ControlMessage;
import org.opendsb.messaging.Message;
import org.opendsb.messaging.MessageType;
import org.opendsb.messaging.control.ControlMessageType;
import org.opendsb.messaging.control.ControlTokens;
import org.opendsb.routing.Router;
import org.opendsb.routing.remote.ws.WebSocketPeer;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public abstract class RemotePeer {
	
	private static final Logger logger = Logger.getLogger(RemotePeer.class);

	protected String address;
	protected String peerId;
	
	protected String connectionId = "";
	
	protected boolean wireConnected = false;
	
	protected boolean busConnected = false;
	
	private String pendingBusConnectionId;
	
	private List<CompletableFuture<Void>> connectedFutures = new ArrayList<>();
	private List<CompletableFuture<Void>> disconnectedFutures = new ArrayList<>();
	
	protected Map<String, Integer> remoteRoutingTableCounter = new ConcurrentHashMap<>();

	protected Router router;
	
	protected RemotePeer(Router router, String address) {
		super();
		this.router = router;
		this.address = address;
		remoteRoutingTableCounter.put("control", 1);
	}
	
	public String getAddress() {
		return address;
	}

	public boolean isBusConnected() {
		return busConnected;
	}
	
	public boolean isWireConnected() {
		return wireConnected;
	}

	public String getPeerId() {
		return peerId;
	}

	public String getConnectionId() {
		return connectionId;
	}

	public void setPeerId(String peerId) {
		this.peerId = peerId;
	}
	
	public Map<String, Integer> getRemoteRoutingTableCounter() {
		return remoteRoutingTableCounter;
	}
	
	public void setConnectionId(String connectionId) {
		this.connectionId = connectionId;
	}

	public void addConnectedFuture(CompletableFuture<Void> connectedFuture) {
		synchronized (connectedFutures) {
			connectedFutures.add(connectedFuture);
		}
	}
	
	public void addDisconnectedFuture(CompletableFuture<Void> disconnectedFuture) {
		synchronized (disconnectedFutures) {
			disconnectedFutures.add(disconnectedFuture);
		}
	}

	public RemotePeerConnection connect() throws IOException {
		
		RemotePeerConnection remotePeerConnection = wireConnect();
		
		pendingBusConnectionId = "ConnectionRequest_" + UUID.randomUUID();
		
		Message connectionRequest = new ControlMessage.Builder()
			.createConnectionRequestMessage(pendingBusConnectionId, router.getId())
			.addClientId(router.getId())
			.addRoutingTableCount(router.getFullSubscriptionCount())
			.build();

		router.routeMessageToPeer(connectionRequest, this);
		
		return remotePeerConnection;
	};
	
	protected abstract RemotePeerConnection wireConnect() throws IOException;

	protected abstract void wireSendMessage(Message message);

	protected abstract void wireCloseConnection(int code, String reason);

	public void disconnect() {
		wireCloseConnection(1001, "Client is disconnecting");
	}
	
	public void connectionOpenned() {
		wireConnected = true;
		router.addPeer(this);
	}
	
	public void connectionClosed(int code, String reason) {
		wireConnected = false;
		busConnected = false;
		router.removePeer(this);
		notifyDisconnection();
	}
	
	public void messageReceived(Message message) {
		
		if (message.getType() == MessageType.CALL) {
			String replyTo = ((CallMessage)message).getReplyTo();
			synchronized (remoteRoutingTableCounter) {
				remoteRoutingTableCounter.put(replyTo, 1);
			}
		}
		
		if (message.getType() == MessageType.CONTROL 
				&& message instanceof ControlMessage) {
			process((ControlMessage) message);
			return;
		}
		
		router.routeMessage(message, true);
	}
	
	public void sendMessage(Message message) {
		
		boolean interested;

		String destination = message.getDestination();

		interested = isRemotePeerInterested(destination);

		if (!interested) {
			logger.debug("No listeners registered for '" + destination + "' in remote peer. Skipping!");
			return;
		}

		logger.debug("Sending remote message to '" + destination + "'");
		wireSendMessage(message);
	}

	private boolean isRemotePeerInterested(String destination) {
		boolean interested = false;

		// Lookup indexes instead
		String[] pieces = destination.split("/");
		String partialDestination = "";

		// Generate the path like a/b/c in a, a/b, a/b/c
		for (int i = 0; i < pieces.length; i++) {
			partialDestination = partialDestination + pieces[i];
			synchronized (remoteRoutingTableCounter) {
				if (remoteRoutingTableCounter.containsKey(destination) && remoteRoutingTableCounter.get(destination) > 0) {
					interested = true;
					break;
				}
			}
			partialDestination = partialDestination + "/";
		}

		return interested;
	}

	protected void process(ControlMessage message) {
		logger.trace("Processing control message of type '" + message.getControlMessageType() + "'");
		
		if ("control".equals(message.getDestination())) {
			if (message.getControlMessageType() == ControlMessageType.UPDATE_ROUTE_COUNT) {
				Type routeTableCount = new TypeToken<Map<String, Integer>>() {}.getType();
				Gson gson = new Gson();
				Map<String, Integer> remoteRoutingTableCounter = gson.fromJson(message.getControlInfo(ControlTokens.ROUTING_TABLE_COUNT), routeTableCount);
				this.remoteRoutingTableCounter = remoteRoutingTableCounter;
				return;
			}
			if (message.getControlMessageType() == ControlMessageType.CONNECTION_REQUEST) {
				doConnectionRequest(message);
				return;
			}
			if (message.getControlMessageType() == ControlMessageType.CONNECTION_REPLY) {
				doConnectionReply(message);
				return;
			}
			
			doProcess(message);
		}
		
		router.routeMessage(message, false);
	}
	
	protected void doProcess(ControlMessage message) {
		return;
	}
	

	protected void doConnectionRequest(ControlMessage message) {

		logger.debug("Receiving connection request connection id '" + connectionId + "'");

		try {
			if (this.connectionId.equals(connectionId)) {
				String clientId = message.getControlInfo(ControlTokens.CLIENT_ID);
				String transactionId = message.getControlInfo(ControlTokens.TRANSACTION_ID);
				Type routeTableCount = new TypeToken<Map<String, Integer>>() {}.getType();
				Gson gson = new Gson();
				Map<String, Integer> remoteRoutingTableCounter = gson.fromJson(message.getControlInfo(ControlTokens.ROUTING_TABLE_COUNT), routeTableCount);
				peerId = clientId;
				setRemoteRoutingTableCounter(remoteRoutingTableCounter);
				Message connectionReply = new ControlMessage.Builder()
					.createConnectionReplyMessage(transactionId, router.getId())
					.addRoutingTableCount(router.getFullSubscriptionCount())
					.addServerId(router.getId())
					.build();
				router.routeMessageToPeer(connectionReply, this);
				busConnected = true;
			} else {
				logger.error("Cannot complete connection request '" + connectionId + "' pending request not found.");
			}

		} catch (Exception e) {
			logger.error("Failure processing a connection request reply.", e);
		}
	}
	

	protected synchronized void doConnectionReply(ControlMessage message) {

		logger.debug("Receiving a connection request reply '" + connectionId + "'");
		
		try {

			if (pendingBusConnectionId.equals(message.getControlInfo(ControlTokens.TRANSACTION_ID))) {
				String serverId = message.getControlInfo(ControlTokens.SERVER_ID);
				Type routeTableCount = new TypeToken<Map<String, Integer>>() {}.getType();
				Gson gson = new Gson();
				Map<String, Integer> remoteRoutingTableCounter = gson.fromJson(message.getControlInfo(ControlTokens.ROUTING_TABLE_COUNT), routeTableCount);
				this.peerId = serverId;
				setRemoteRoutingTableCounter(remoteRoutingTableCounter);
				busConnected = true;
				notifyConnectionSuccess();
			} else {
				logger.warn("Received a connection reply from unknown source. Ignoring.");
			}

		} catch (Exception e) {
			logger.error("Failure processing a connection request reply.", e);
			notifyConnectionFailure(e);
		}
	}
	
	protected void setRemoteRoutingTableCounter(Map<String, Integer> remoteRoutingTableCounter) {
		synchronized (this.remoteRoutingTableCounter) {
			this.remoteRoutingTableCounter.clear();
			this.remoteRoutingTableCounter.putAll(remoteRoutingTableCounter);
			this.remoteRoutingTableCounter.put("control", 1);
			if (logger.isTraceEnabled()) {
				logger.trace("refreshing remote peer table count");
				for (String topic : this.remoteRoutingTableCounter.keySet()) {
					logger.trace("Topic: '" + topic + "': " + this.remoteRoutingTableCounter.get(topic));
				}
			}
		}
	}
	
	protected void notifyConnectionFailure(Throwable exception) {
		synchronized (connectedFutures) {
			Iterator<CompletableFuture<Void>> it = connectedFutures.iterator();
			
			while(it.hasNext()) {
				CompletableFuture<Void> cf = it.next();
				cf.completeExceptionally(exception);
				it.remove();
			}
		}
	}
	
	protected void notifyConnectionSuccess() {
		synchronized (connectedFutures) {
			Iterator<CompletableFuture<Void>> it = connectedFutures.iterator();
			
			while(it.hasNext()) {
				CompletableFuture<Void> cf = it.next();
				cf.complete(null);
				it.remove();
			}
		}
	}
	
	protected void notifyDisconnection() {
		synchronized (disconnectedFutures) {
			Iterator<CompletableFuture<Void>> it = disconnectedFutures.iterator();
			
			while(it.hasNext()) {
				CompletableFuture<Void> cf = it.next();
				cf.complete(null);
				it.remove();
			}
		}
	}
	
	public static class Builder {

		public RemotePeer build(Router router, String remoteAddress, Map<String, Object> opt) throws IllegalArgumentException {

			if (remoteAddress.startsWith("ws://")) {
				
				String sessionCookie = "";
				
				if (opt != null && opt.containsKey("sessionCookie")) {
					sessionCookie = (String)opt.get("sessionCookie");
				}
				
				return new WebSocketPeer(router, remoteAddress, sessionCookie);
			}

			throw new IllegalArgumentException("Address string '" + remoteAddress + "' not recognized");
		}
	}
}
