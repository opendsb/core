package org.dsb.memory.utils;

import java.util.function.Consumer;

public class ServiceAndHandler implements Consumer<String> {
	
	private String topic;
	
	private StubRouter router;
	
	private Subscription subscription;

	public ServiceAndHandler(String topic, StubRouter router) {
		super();
		this.topic = topic;
		this.router = router;
	}
	
	public void subscribe() {
		subscription = router.subscribe(topic, this);
	}

	@Override
	public void accept(String t) {
		System.out.println("Hey I am a service and a listener watch me '" + t + "'.");
	}
}
