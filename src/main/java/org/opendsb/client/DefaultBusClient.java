package org.opendsb.client;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.opendsb.messaging.CallMessage;
import org.opendsb.messaging.ControlMessage;
import org.opendsb.messaging.DataMessage;
import org.opendsb.messaging.Message;
import org.opendsb.messaging.ReplyMessage;
import org.opendsb.messaging.Subscription;
import org.opendsb.messaging.control.ControlMessageType;
import org.opendsb.messaging.control.ControlTokens;
import org.opendsb.routing.HandlerPriority;
import org.opendsb.routing.Router;

public class DefaultBusClient implements BusClient {
	
	private static final Logger logger = Logger.getLogger(DefaultBusClient.class);
	
	private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, (r) -> {
		Thread thread = Executors.defaultThreadFactory().newThread(r);
		thread.setName("OpenDSB-Client [" + thread.getName() + "]");
		thread.setDaemon(true);
		return thread;
	});
	
	@SuppressWarnings("unused")
	private final String clientId = "Client_" + UUID.randomUUID().toString();

	private Router router;
	
	private long timeoutMills = 1000L;
 
	public static BusClient of(Router router) {
		return new DefaultBusClient(router);
	}

	private DefaultBusClient(Router router) {
		this.router = router;
	}
	
	public long getTimeoutMills() {
		return timeoutMills;
	}

	public void setTimeoutMills(long timeoutMills) {
		this.timeoutMills = timeoutMills;
	}

	@Override
	public CompletableFuture<ReplyMessage> call(String methodTopic, List<Object> parameters) {

		String replyTo = "reply-" + UUID.randomUUID().toString() + "/" + methodTopic;
		
		CallMessage callMsg = new CallMessage(methodTopic, router.getId(), parameters, replyTo);
		
		Caller response = new Caller(this, timeoutMills, methodTopic, replyTo, callMsg.getMessageId());	
		
		logger.info("Routing call message to '" + methodTopic + "' - '" + replyTo + "'");
		
		router.routeMessage(callMsg, true);

		return response;
	}
	
	private static class Caller extends CompletableFuture<ReplyMessage> {
		
		private Subscription replyTopic;
		
		private ScheduledFuture<?> timeoutTask;
		
		public Caller(BusClient busClient, Long timeoutMills, String topic, String replyTo, String transactionId) {
			super();
			replyTopic = busClient.subscribe(replyTo, (msg) -> {
				if (msg instanceof ControlMessage) {
					ControlMessage cMsg = (ControlMessage)msg;
					if(cMsg.getControlMessageType().equals(ControlMessageType.CALL_ACK) && cMsg.getControlInfo(ControlTokens.TRANSACTION_ID).equals(transactionId)) {
						logger.info("Request for '" + topic + "' acknowledged - " + replyTo + "'");
						timeoutTask.cancel(true);
					}
					return;
				}
				if (msg instanceof ReplyMessage) {
					this.complete((ReplyMessage)msg);
					replyTopic.cancel();
				}
			});
			timeoutTask = executor.schedule(() -> {
				logger.info("Request for '" + topic + "' timed out - " + replyTo + "'");
				replyTopic.cancel();
				this.completeExceptionally(new RuntimeException("Request for '" + topic + "' timed out"));
			}, timeoutMills, TimeUnit.MILLISECONDS);
		}
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
