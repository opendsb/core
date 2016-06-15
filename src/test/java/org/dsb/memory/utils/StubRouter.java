package org.dsb.memory.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;

public class StubRouter {
	
	
	private Map<String, Map<Subscription, Object>> subs = new HashMap<>();
	
	
	public Subscription subscribe(String topic, Consumer<String> lambda) {
		
		Subscription subscription = null;
		Map<Subscription, Object> subMap;
		
		if (subs.containsKey(topic)) {
			subMap = subs.get(topic);
		} else {
			subMap = new WeakHashMap<>();
			subs.put(topic, subMap);
		}
		
		subscription = new Subscription(topic, lambda, this);
		
		subMap.put(subscription, null);
		
		return subscription;
	}
	
	public void cancelSubscription(Subscription subscription) {
		Map<Subscription, Object> subMap;
		
		if (subs.containsKey(subscription.getTopic())) {
			subMap = subs.get(subscription.getTopic());
			if (subMap.containsKey(subscription)) {
				subMap.remove(subscription);
			}
		}
	}

	public void routeMessage(String topic, String message) {
		Map<Subscription, Object> subMap;
		
		if (subs.containsKey(topic)) {
			subMap = subs.get(topic);
			subMap.keySet().stream().forEach(s -> s.accept(message));
		}
	}
}
