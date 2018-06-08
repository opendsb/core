package org.opendsb.routing.remote;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;
import org.opendsb.messaging.Message;
import org.opendsb.routing.remote.ws.WebSocketPeer;

public abstract class RemotePeer {
	
	private static final Logger logger = Logger.getLogger(RemotePeer.class);

	protected boolean shutdown = false;

	protected String address;
	protected String peerId;

	protected Map<String, Integer> remoteRoutingTableCounter = new ConcurrentHashMap<>();
	
	protected String connectionId;
	
	protected boolean connected = false;
	
	protected boolean pending = true;

	protected RemoteRouter router;
	
	protected RemotePeer(String address, RemoteRouter router) {
		super();
		this.address = address;
		this.router = router;
		remoteRoutingTableCounter.put("control", 1);
	}
	
	public String getAddress() {
		return address;
	}

	public void activate() {
		connected = true;
		pending = false;
	}

	public boolean isConnected() {
		return connected;
	}

	public boolean isPending() {
		return pending;
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

	public void setRemoteRoutingTableCounter(Map<String, Integer> remoteRoutingTableCounter) {
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

	public abstract void connect() throws IOException;

	public void sendMessage(Message message) {
		// Lookup indexes instead
		String[] pieces = message.getDestination().split("/");
		String destination = null;
		
		String concat = "";
		
		boolean send = false;
		
		// Generate the path like a/b/c in a, a/b, a/b/c
		for (int i = 0; i < pieces.length; i++) {
			destination = concat + pieces[i];
			if (remoteRoutingTableCounter.containsKey(destination) && remoteRoutingTableCounter.get(destination) > 0) {
				send = true;
				break;
			}
			concat = destination + "/";
		}
		if (send) {
			doSendMessage(message);
		}
	}
	
	public abstract void doSendMessage(Message message);

	public abstract void closeConnection();

	public abstract void closeConnection(int code, String reason);

	public void shutdown() {
		shutdown = true;
		closeConnection();
	}

	public void onMessage(Message message) {
		router.receiveMessage(connectionId, message);
	}
	
	public static class Builder {

		public RemotePeer build(String remoteAddress, RemoteRouter router, String sessionCookie) throws IllegalArgumentException {

			if (remoteAddress.startsWith("ws://")) {
				return new WebSocketPeer(remoteAddress, sessionCookie, router);
			}

			throw new IllegalArgumentException("Address string '" + remoteAddress + "' not recognized");
		}
	}
}
