package org.opendsb.routing.remote;

import java.util.concurrent.CompletableFuture;

public class RemotePeerConnection {
	
	private RemotePeer remotePeer;
	
	public RemotePeerConnection(RemotePeer remotePeer) {
		super();
		this.remotePeer = remotePeer;
	}
	
	public RemotePeer getRemotePeer() {
		return remotePeer;
	}

	public boolean isWireConnected() {
		return remotePeer.isWireConnected();
	}
	
	public boolean isBusConnected() {
		return remotePeer.isBusConnected();
	}

	public synchronized CompletableFuture<Void> whenConnected() {
		
		CompletableFuture<Void> connectedFuture = new CompletableFuture<>();
		
		if(remotePeer != null && remotePeer.isBusConnected()) {
			 connectedFuture.complete(null);
		} else {
			remotePeer.addConnectedFuture(connectedFuture);
		}
		
		return connectedFuture;
	}
	
	public CompletableFuture<Void> whenDisconnected() {
		
		CompletableFuture<Void> disconnectedFuture = new CompletableFuture<>();

		if(remotePeer == null || !remotePeer.isBusConnected()) {
			 disconnectedFuture.complete(null);
		} else {
			remotePeer.addDisconnectedFuture(disconnectedFuture);
		}
		
		return disconnectedFuture;
	}

}
