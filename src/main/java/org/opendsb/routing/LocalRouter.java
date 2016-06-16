package org.opendsb.routing;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.opendsb.messaging.Message;
import org.opendsb.messaging.Subscription;
import org.opendsb.routing.remote.RemoteRouter;

public class LocalRouter implements Router {

	private static final Logger logger = Logger.getLogger(LocalRouter.class);

	private ExecutorService executorService = Executors.newFixedThreadPool(5);

	private RemoteRouter remoteRouter;

	private CompositeRouteNode routeTree;

	private String routerID = "Router_" + UUID.randomUUID();

	public LocalRouter() {
		super();
		routeTree = new CompositeRouteNode("Root");
	}

	protected CompositeRouteNode getRouteTree() {
		return routeTree;
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
		RoutingTask task = new RoutingTask(routeTree, message);
		if (remoteBroadCast) {
			task.setRemoteRouter(remoteRouter);
		}
		executorService.submit(task);
	}

	@Override
	public Subscription subscribe(String topic, Consumer<Message> handler, HandlerPriority priority) {

		logger.debug("subscribing to topic '" + topic + "'");

		String[] nodes = topic.split("/");

		CompositeRouteNode node = routeTree;
		RouteNode ref;

		Subscription subscription = null;

		for (int i = 0; i < nodes.length; i++) {
			String targetId = nodes[i];
			List<RouteNode> list = node.getChildren().filter(n -> n.getNodeId().equals(targetId))
					.collect(Collectors.toList());

			if (list.size() > 1) {
				logger.warn("More than one node with an ID of '" + targetId + "'.");
			}

			if (list.size() <= 0) {
				ref = new CompositeRouteNode(targetId);
				node.addChild(ref);
			} else {
				ref = list.get(0);
			}

			// Only the last element in the chain gets the handler
			if (i == nodes.length - 1) {
				subscription = ref.subscribe(topic, handler, priority);
			} else {
				node = (CompositeRouteNode) ref;
			}
		}

		return subscription;
	}

}
