package org.opendsb.routing;

import org.apache.log4j.Logger;
import org.dsb.pattern.visitor.TreeVisitor;
import org.opendsb.messaging.Message;

public class RequestDispatcher implements TreeVisitor<RouteNode>, Dispatcher {
	
	private static Logger logger = Logger.getLogger(RequestDispatcher.class);

	private Message message;
	
	private String[] splitAddr;
	
	private int index = 0;
	
	// Starts at -1 because of route node. 
	private int depth = -1;
	
	private boolean present = false;
	
	public RequestDispatcher(Message message) {
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
	
	public boolean isPresent() {
		return present;
	}

	@Override
	public void visit(RouteNode host) {
		if (index == splitAddr.length - 1) {
			logger.info("Dispatching call message to node '" + host.getNodeId() + "'");
			present = host.accept(message);
		}
		if(depth == index) {
			logger.info("Acknowledging node '" + host.getNodeId() + "'");
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
