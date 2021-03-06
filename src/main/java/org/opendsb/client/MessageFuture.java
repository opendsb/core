package org.opendsb.client;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.opendsb.messaging.ControlMessage;
import org.opendsb.messaging.Message;
import org.opendsb.messaging.Subscription;
import org.opendsb.messaging.control.ControlMessageType;
import org.opendsb.messaging.control.ControlTokens;

@SuppressWarnings("unchecked")
public class MessageFuture<T extends Message> implements Future<T>, Consumer<T> {

	private BlockingQueue<T> waitingQueue = new ArrayBlockingQueue<>(1);

	private Set<Thread> blockedThreads = new HashSet<>();
	
	private String transactionId;

	private boolean done;

	private boolean cancelled;
	
	private boolean acknowledged = false;

	private Subscription replySubscription;

	public MessageFuture(BusClient busClient, String replyTopic, String transactionId) {
		this.transactionId = transactionId;
		this.replySubscription = busClient.subscribe(replyTopic, (Consumer<Message>) this);
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		if (mayInterruptIfRunning) {
			blockedThreads.stream().forEach(t -> t.interrupt());
			replySubscription.cancel();
		}
		cancelled = true;
		return true;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public boolean isDone() {
		return done;
	}

	public boolean isAcknowledged() {
		return acknowledged;
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		T reply;
		Thread ct = Thread.currentThread();
		blockedThreads.add(ct);
		try {
			reply = waitingQueue.take();
		} finally {
			blockedThreads.remove(ct);
		}
		return reply;
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		T repl = null;
		Thread ct = Thread.currentThread();
		blockedThreads.add(ct);
		try {
			repl = waitingQueue.poll(timeout, unit);
			if (repl == null) {
				throw new TimeoutException();
			}
		} finally {
			blockedThreads.remove(ct);
		}
		return repl;
	}

	@Override
	public void accept(T msg) {
		if (msg instanceof ControlMessage) {
			ControlMessage cMsg = (ControlMessage)msg;
			if(cMsg.getControlMessageType().equals(ControlMessageType.CALL_ACK) && cMsg.getControlInfo(ControlTokens.TRANSACTION_ID).equals(transactionId)) {
				acknowledged = true;
			}
			return;
		}
		waitingQueue.add(msg);
		done = true;
	}
}
