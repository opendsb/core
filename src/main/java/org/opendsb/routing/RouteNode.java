package org.opendsb.routing;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.opendsb.messaging.Message;
import org.opendsb.messaging.Subscription;
import org.opendsb.pattern.composite.Part;
import org.opendsb.pattern.visitor.Host;
import org.opendsb.pattern.visitor.TreeVisitor;

public class RouteNode implements Part<RouteNode>, Comparable<RouteNode>, Host<RouteNode> {

	private static final Logger logger = Logger.getLogger(RouteNode.class);

	private String nodeId;

	private Map<Subscription, Object> subscribers = new WeakHashMap<>();

	private RouteNode parent;

	public RouteNode(String nodeId) {
		super();
		this.nodeId = nodeId;
	}

	@Override
	public int compareTo(RouteNode o) {
		return this.nodeId.compareTo(o.getNodeId());
	}

	public String getNodeId() {
		return nodeId;
	}

	public boolean accept(Message message) {
		logger.trace("Accepting message for destination: '" + message.getDestination() + "' at node '" + nodeId + "'.");
		if (subscribers.size() == 0) {
			return false;
		}
		logger.trace("Iterating through node '" + nodeId + "'");
		// Parallel processing hook point
		subscribers.keySet().stream().sorted().forEach(h -> h.getConsumer().accept(message));
		return true;
	}

	public Subscription subscribe(String topic, Consumer<Message> consumer, HandlerPriority priority) {
		Subscription subscription = new Subscription(topic, this, consumer, priority);
		subscribers.put(subscription, null);
		return subscription;
	}

	public void cancelSubscription(Subscription subscription) {
		subscribers.remove(subscription);
	}

	public RouteNode getParent() {
		return parent;
	}

	public void setParent(RouteNode parent) {
		this.parent = parent;
	}

	@Override
	public void accept(TreeVisitor<RouteNode> visitor) {
		visitor.visit(this);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((nodeId == null) ? 0 : nodeId.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		RouteNode other = (RouteNode) obj;
		if (nodeId == null) {
			if (other.nodeId != null) {
				return false;
			}
		} else if (!nodeId.equals(other.nodeId)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "RouteNode [nodeId=" + nodeId + "]";
	}
}
