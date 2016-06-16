package org.opendsb.client;

import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import org.opendsb.messaging.CallMessage;
import org.opendsb.messaging.DataMessage;
import org.opendsb.messaging.Message;
import org.opendsb.messaging.ReplyMessage;
import org.opendsb.messaging.Subscription;
import org.opendsb.routing.HandlerPriority;
import org.opendsb.routing.Router;

public class DefaultBusClient implements BusClient {

	private Router router;

	public static BusClient of(Router router) {
		return new DefaultBusClient(router);
	}

	private DefaultBusClient(Router router) {
		this.router = router;
	}

	@Override
	public MessageFuture<ReplyMessage> call(String methodTopic, Map<String, Object> parameters) {
		MessageFuture<ReplyMessage> response = null;

		String replyTo = "reply-" + UUID.randomUUID().toString() + "/" + methodTopic;

		response = new MessageFuture<>(this, replyTo);

		CallMessage callMsg = new CallMessage(methodTopic, router.getId(), parameters, replyTo);

		router.routeMessage(callMsg, true);

		return response;
	}

	@Override
	public void publishData(String topic, Object data) {
		Message dataMessage = new DataMessage(topic, router.getId(), data);
		router.routeMessage(dataMessage, true);
	}

	@Override
	public void publishReply(String topic, Object reply) {
		Message replyMessage = new ReplyMessage(topic, router.getId(), reply);
		router.routeMessage(replyMessage, true);
	}

	@Override
	public void postFailureReply(String topic, String reason) {
		Message replyMessage = new ReplyMessage(topic, router.getId(), reason);
		router.routeMessage(replyMessage, true);
	}

	@Override
	public Subscription subscribe(String topic, Consumer<Message> handler) {
		return subscribe(topic, handler, HandlerPriority.NORMAL);
	}

	@Override
	public Subscription subscribe(String topic, Consumer<Message> handler, HandlerPriority priority) {
		return router.subscribe(topic, handler, priority);
	}
}
