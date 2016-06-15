package org.opendsb.messaging;

import java.util.function.Consumer;

import org.opendsb.routing.HandlerPriority;
import org.opendsb.routing.RouteNode;

public class Subscription implements Comparable<Subscription> {

	private String topic;
	
	private RouteNode node;
	
	private HandlerPriority priority;
	
	private Consumer<Message> consumer;

	public Subscription(String topic, RouteNode node, Consumer<Message> consumer) {
		super();
		this.node = node;
		this.topic = topic;
		this.consumer = consumer;
		this.priority = HandlerPriority.NORMAL;
	}

	public Subscription(String topic, RouteNode node, Consumer<Message> consumer,
			HandlerPriority priority) {
		super();
		this.node = node;
		this.topic = topic;
		this.consumer = consumer;
		this.priority = priority;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public Consumer<Message> getConsumer() {
		return consumer;
	}

	public HandlerPriority getPriority() {
		return priority;
	}

	public void setPriority(HandlerPriority priority) {
		this.priority = priority;
	}
	
	public void cancel() {
		node.cancelSubscription(this);
	}

	@Override
	public int compareTo(Subscription o) {
		return priority.ordinal() - o.getPriority().ordinal();
	}
}
