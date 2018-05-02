package org.opendsb.routing;

import org.apache.log4j.Logger;
import org.opendsb.messaging.CallMessage;
import org.opendsb.messaging.ControlMessage;
import org.opendsb.messaging.Message;
import org.opendsb.pattern.visitor.TreeVisitor;

public class RequestDispatcher implements TreeVisitor<RouteNode>, Dispatcher {

	private static Logger logger = Logger.getLogger(RequestDispatcher.class);

	private CallMessage message;

	private String[] splitAddr;

	private int index = 0;

	// Starts at -1 because of root node.
	private int depth = -1;

	private boolean present = false;
	
	private Router localRouter;

	public RequestDispatcher(Router localRouter, CallMessage message) {
		super();
		this.message = message;
		this.localRouter = localRouter;
		splitAddr = message.getDestination().split("/");
	}

	public CallMessage getMessage() {
		return message;
	}

	public void setMessage(CallMessage message) {
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
			if (host.subscriptionCount() > 0) {
				ControlMessage ack = new ControlMessage.Builder().createCallAckMessage(message.getMessageId(), localRouter + "/" + host.getFullNodeId(), message.getReplyTo()).build();
				localRouter.routeMessage(ack, true);
			}
			present = host.accept(message);
		}
		if (depth == index) {
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
