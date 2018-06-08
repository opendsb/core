package org.opendsb.client;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.opendsb.messaging.Message;
import org.opendsb.messaging.ReplyMessage;
import org.opendsb.messaging.Subscription;
import org.opendsb.routing.HandlerPriority;

public interface BusClient {
	public void publishData(String topic, Object data);

	public Subscription subscribe(String topic, Consumer<Message> handler);

	public Subscription subscribe(String topic, Consumer<Message> handler, HandlerPriority priority);

	public CompletableFuture<ReplyMessage> call(String methodTopic, Map<String, Object> parameters);

	public void publishReply(String topic, Object reply);

	public void postFailureReply(String topic, String reason);
}
