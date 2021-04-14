package org.opendsb.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.opendsb.messaging.CallMessage;
import org.opendsb.messaging.ControlMessage;
import org.opendsb.messaging.Message;
import org.opendsb.messaging.Subscription;

public class RouteNode implements Comparable<RouteNode> {

	private static final Logger logger = Logger.getLogger(RouteNode.class);

	private String topic;

	private Map<String, Subscription> subscribers = new HashMap<>();
	
	private Router localRouter;

	public RouteNode(String topic, Router localRouter) {
		super();
		this.topic = topic;
		this.localRouter = localRouter;
	}

	@Override
	public int compareTo(RouteNode o) {
		return this.topic.compareTo(o.getTopic());
	}

	public String getTopic() {
		return topic;
	}
	
	public int subscriptionCount() {
		return subscribers.size();
	}

	public boolean accept(Message message) {
		logger.trace("Accepting message for destination: '" + message.getDestination() + "' at node '" + topic + "'.");
		
		switch (message.getType()) {
		case CALL: {
			CallMessage msg = (CallMessage)message;
			ControlMessage ack = new ControlMessage.Builder().createCallAckMessage(msg.getMessageId(), localRouter.getId() + "/" + topic, msg.getReplyTo()).build();
			localRouter.routeMessage(ack, true);
			break;
		}

		default: {
			
		}
		}
		
		// Parallel processing hook point
		subscribers.values().stream().sorted().forEach(h -> {
			try{
				h.getConsumer().accept(message);
			} catch (Exception e) {
				logger.error("Uanble to process message", e);
			}
		});
		return true;
	}

	public Subscription subscribe(Consumer<Message> consumer, HandlerPriority priority) {
		Subscription subscription = new Subscription(topic, this, consumer, priority);
		subscribers.put(subscription.getId(), subscription);
		notifySubCountChange();
		return subscription;
	}

	public void cancelSubscription(Subscription subscription) {
		subscribers.remove(subscription.getId());
		notifySubCountChange();
	}
	
	private void notifySubCountChange() {
		logger.trace("Notifying subscription status change to topic '" + topic  + "'");
		localRouter.routeMessage(new ControlMessage.Builder()
				.createUpdateRouteCountMessage(UUID.randomUUID().toString(), topic + "@" + localRouter.getId())
				.addRoutingTableCount(localRouter.getFullSubscriptionCount())
				.build(), true);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((topic == null) ? 0 : topic.hashCode());
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
		if (topic == null) {
			if (other.topic != null) {
				return false;
			}
		} else if (!topic.equals(other.topic)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "RouteNode [nodeId=" + topic + "]";
	}
}
