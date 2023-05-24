package org.opendsb.routing;

import java.io.IOException;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.opendsb.messaging.Message;
import org.opendsb.messaging.Subscription;
import org.opendsb.routing.remote.RemotePeer;
import org.opendsb.routing.remote.RemotePeerConnection;

public interface Router {
	
	public String getId();
	
	public void addPeer(RemotePeer peer) throws IllegalArgumentException;
	
	public RemotePeer getPeer(String connectionId);

	public void removePeer(RemotePeer peer);
	
	public Stream<RemotePeer> peerStream();
	
	public Map<String, RouteNode> getRoutingTable();
	
	public Map<String, Integer> getFullSubscriptionCount();

	public void routeMessage(Message message, boolean remote);

	public void routeMessageToPeer(Message message, RemotePeer peer);

	public void removeConnectionListener(long listenerCode);

	public long addConnectionListener(Consumer<RemotePeer> listener);

	public Subscription subscribe(String topic, Consumer<Message> handler, HandlerPriority priority);
	
	public RemotePeerConnection connectToRemoteRouter(String address, Map<String, Object> opt) throws IOException;

	
	public static Router newRouter() {
		return new DefaultRouter();
	}
	
	public static Router newRouter(int numberOfThreads) {
		return new DefaultRouter(numberOfThreads);
	}
}
