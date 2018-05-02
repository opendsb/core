package org.opendsb.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.opendsb.messaging.CallMessage;
import org.opendsb.messaging.DataMessage;
import org.opendsb.messaging.Message;
import org.opendsb.messaging.ReplyMessage;
import org.opendsb.messaging.Subscription;
import org.opendsb.pattern.action.Action;
import org.opendsb.routing.HandlerPriority;
import org.opendsb.routing.Router;

public class DefaultBusClient implements BusClient {
	
	private final String clientId = "Client_" + UUID.randomUUID().toString();

	private Router router;
	
	private long timeoutMills = 1000L;
	
	private ScheduledExecutorService executor;

	public static BusClient of(Router router) {
		return new DefaultBusClient(router);
	}

	private DefaultBusClient(Router router) {
		this.router = router;
		this.executor = Executors.newScheduledThreadPool(2, (r) -> {
			Thread thread = Executors.defaultThreadFactory().newThread(r);
			thread.setName("OpenDSB-" + clientId + "[" + thread.getName() + "]");
			thread.setDaemon(true);
			return thread;
		});
	}
	
	public long getTimeoutMills() {
		return timeoutMills;
	}

	public void setTimeoutMills(long timeoutMills) {
		this.timeoutMills = timeoutMills;
	}

	@Override
	public MessageFuture<ReplyMessage> call(String methodTopic, Map<String, Object> parameters, Action noServiceFoundCallback) {

		String replyTo = "reply-" + UUID.randomUUID().toString() + "/" + methodTopic;
		
		CallMessage callMsg = new CallMessage(methodTopic, router.getId(), parameters, replyTo);
		
		final MessageFuture<ReplyMessage> response = new MessageFuture<>(this, replyTo, callMsg.getMessageId());
		
		executor.schedule(() -> {
			if (!response.isAcknowledged()) {
				response.cancel(true);
				noServiceFoundCallback.execute();
			}
		}, timeoutMills, TimeUnit.MILLISECONDS);

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
