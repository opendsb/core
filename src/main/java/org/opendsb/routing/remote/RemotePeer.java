package org.opendsb.routing.remote;

import java.io.IOException;
import java.net.URI;

import org.opendsb.messaging.Message;
import org.opendsb.routing.remote.ws.WebSocketPeer;

public abstract class RemotePeer {

	protected boolean shutdown = false;

	protected URI address;
	protected String peerId;

	protected String connectionId;

	protected RemoteRouter router;

	protected RemotePeer(String address, RemoteRouter router) {
		super();
		this.address = URI.create(address);
		this.router = router;
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

	public abstract void connect() throws IOException;

	public abstract void sendMessage(Message message);

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

		public RemotePeer build(String remoteAddress, RemoteRouter router) throws IllegalArgumentException {

			if (remoteAddress.startsWith("ws://")) {
				return new WebSocketPeer(remoteAddress, router);
			}

			throw new IllegalArgumentException("Address string '" + remoteAddress + "' not recognized");
		}
	}
}
