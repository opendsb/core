package org.opendsb.routing;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.opendsb.messaging.Message;
import org.opendsb.messaging.Subscription;
import org.opendsb.routing.remote.RemoteRouter;

public class LocalRouter implements Router {

	private static final Logger logger = Logger.getLogger(LocalRouter.class);

	private ExecutorService executorService = Executors.newFixedThreadPool(5);

	private RemoteRouter remoteRouter;

	protected Map<String, RouteNode> routingTable = new ConcurrentHashMap<>();

	private String routerID = "Router_" + UUID.randomUUID();

	public LocalRouter() {
		super();
		executorService = Executors.newFixedThreadPool(5, (r) -> {
			Thread thread = Executors.defaultThreadFactory().newThread(r);
			thread.setName("OpenDSB-" + routerID + "[" + thread.getName() + "]");
			thread.setDaemon(true);
			return thread;
		});
	}
	
	public LocalRouter(int numberOFThreads) {
		super();
		executorService = Executors.newFixedThreadPool(numberOFThreads, (r) -> {
			Thread thread = Executors.defaultThreadFactory().newThread(r);
			thread.setName("OpenDSB-" + routerID + "[" + thread.getName() + "]");
			thread.setDaemon(true);
			return thread;
		});
	}
	
	public Map<String, Integer> getFullSubscriptionCount() {
		Map<String, Integer> fullCount = new HashMap<>();
		for (String topic : routingTable.keySet()) {
			fullCount.put(topic, routingTable.get(topic).subscriptionCount());
		}
		return fullCount;
	}

	@Override
	public String getId() {
		return routerID;
	}

	@Override
	public void setRemoteRouter(RemoteRouter remoteRouter) {
		this.remoteRouter = remoteRouter;
	}

	@Override
	public void routeMessage(Message message, boolean remoteBroadCast) {
		logger.trace("Routing message '" + message.getType() + "' to topic '" + message.getDestination() + "'");
		RoutingTask task = new RoutingTask(routingTable, message);
		if (remoteBroadCast) {
			task.setRemoteRouter(remoteRouter);
		}
		executorService.submit(task);
	}

	@Override
	public Subscription subscribe(String topic, Consumer<Message> handler, HandlerPriority priority) {

		logger.trace("subscribing to topic '" + topic + "'");

		Subscription subscription = null;
		RouteNode subNode = null;
		
		if (routingTable.containsKey(topic)) {
			subNode = routingTable.get(topic);
		} else {
			subNode = new RouteNode(topic, this);
			routingTable.put(topic, subNode);
		}
		
		subscription = subNode.subscribe(handler, priority);
		
		return subscription;
	}

}
