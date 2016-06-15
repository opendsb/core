package org.opendsb.routing;

import org.apache.log4j.Logger;
import org.dsb.pattern.visitor.TreeVisitor;
import org.opendsb.messaging.Message;

public class RouteNodeDispatcher implements TreeVisitor<RouteNode>, Dispatcher {
	
	private static Logger logger = Logger.getLogger(RouteNodeDispatcher.class);

	private Message message;
	
	private String[] splitAddr;
	
	private int index = 0;
	
	// Starts at -1 because of route node. 
	private int depth = -1;	
	
	public RouteNodeDispatcher(Message message) {
		super();
		this.message = message;
		splitAddr = message.getDestination().split("/");
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}
	
	public String getCurrentNode() {
		if (index >= splitAddr.length) {
			return "";
		}
		return splitAddr[index];
	}

	@Override
	public void visit(RouteNode host) {
		if (depth == index) {
			logger.debug("RouteNodeDispatcher -> Dispatching messages to node '" + host.getNodeId() + "'");
			host.accept(message);
			index++;
		}
	}

	@Override
	public void increaseDepth() {
		depth++;
	}

	@Override
	public void decreaseDepth() {
		depth--;
	}
}
