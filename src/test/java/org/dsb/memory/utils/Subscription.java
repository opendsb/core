package org.dsb.memory.utils;

import java.util.function.Consumer;

public class Subscription {

	private String topic;

	private Consumer<String> lambda;
	
	private StubRouter router;
	
	
	public Subscription(String topic, Consumer<String> lambda,
			StubRouter router) {
		super();
		this.topic = topic;
		this.lambda = lambda;
		this.router = router;
	}
	
	public void accept(String s) {
		lambda.accept(s);
	}
	
	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public void cancel() {
		router.cancelSubscription(this);
	}
}
