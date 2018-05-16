package org.opendsb.routing;

import java.util.Map;

import org.opendsb.messaging.Message;
import org.opendsb.routing.remote.RemoteRouter;

public class RoutingTask implements Runnable {

	private Map<String, RouteNode> routingTable;

	private Message message;
	
	private RemoteRouter remoteRouter = null;

	public RoutingTask(Map<String, RouteNode> routingTable, Message message) {
		super();
		this.message = message;
		this.routingTable = routingTable;
	}

	public void setRemoteRouter(RemoteRouter remoteRouter) {
		this.remoteRouter = remoteRouter;
	}

	@Override
	public void run() {

		// Lookup indexes instead
		
		String[] pieces = message.getDestination().split("/");
		
		String concat = "";
		String destination = null;
		// Generate the path like a/b/c in a, a/b, a/b/c
		for (int i = 0; i < pieces.length; i++) {
			destination = concat + pieces[i];
			
			// Do Stuff
			routeMessage(destination);
			
			concat = destination + "/";
		}
		
		// Sends message to remote peers as well.
		if (remoteRouter != null) {
			remoteRouter.sendMessage(message);
		}
	}
	
	private void routeMessage(String destination) {
		if (routingTable.containsKey(destination) && routingTable.get(destination).subscriptionCount() > 0) {
			RouteNode node = routingTable.get(destination);
			node.accept(message);
		}
	}

}
