package org.opendsb.routing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.opendsb.messaging.Message;
import org.opendsb.routing.remote.RemotePeer;

public class RoutingTask implements Runnable {

	private Router router;
	
	private Message message;

	private Collection<RemotePeer> peers = new ArrayList<>();
	

	public RoutingTask(Router router, Message message) {
		super();
		this.message = message;
		this.router = router;
	}

	public void setPeers(Collection<RemotePeer> peers) {
		this.peers = peers;
	}

	@Override
	public void run() {

		// Lookup indexes instead
		String[] pieces = message.getDestination().split("/");
		
		String concat = "";
		String destination = null;
		// From a path like a/b/c get to a set [a, a/b, a/b/c]
		for (int i = 0; i < pieces.length; i++) {
			destination = concat + pieces[i];
			
			// Do Stuff
			routeMessage(destination);
			
			concat = destination + "/";
		}

		// Routes the message to remote peers as well.
		routeMessageToRemotePeers();
	}
	
	private void routeMessageToRemotePeers() {
		String previousHop = message.getLatestHop();
		message.setLatestHop(router.getId());
		// Send the message to all peers except the one that send the message. (Flood strategy)
		peers.stream().filter(peer -> !peer.getPeerId().equals(previousHop))
				.forEach(peer -> {
					// SendMesage -> Already checks if the topic is being listened in the peer else does not propagate.
					peer.sendMessage(message);
				});
	}
	
	private void routeMessage(String destination) {
		Map<String, RouteNode> routingTable = router.getRoutingTable();
		if (routingTable.containsKey(destination) && routingTable.get(destination).subscriptionCount() > 0) {
			RouteNode node = routingTable.get(destination);
			node.accept(message);
		}
	}

}
