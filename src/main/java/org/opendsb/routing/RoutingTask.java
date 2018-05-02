package org.opendsb.routing;

import org.opendsb.messaging.CallMessage;
import org.opendsb.messaging.Message;
import org.opendsb.pattern.navigator.ConditionalVisitor;
import org.opendsb.pattern.navigator.Navigator;
import org.opendsb.pattern.navigator.TreeTopDownNavigator;
import org.opendsb.pattern.visitor.TreeVisitor;
import org.opendsb.routing.remote.RemoteRouter;

public class RoutingTask implements Runnable {

	private CompositeRouteNode routeTree;

	private Message message;
	
	private Router localRouter;

	private RemoteRouter remoteRouter = null;

	public RoutingTask(CompositeRouteNode routeTree, Router localRouter, Message message) {
		super();
		this.message = message;
		this.routeTree = routeTree;
		this.localRouter = localRouter;
	}

	public void setRemoteRouter(RemoteRouter remoteRouter) {
		this.remoteRouter = remoteRouter;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void run() {

		if (remoteRouter != null) {
			remoteRouter.sendMessage(message);
		}

		Navigator<RouteNode> nav = new TreeTopDownNavigator<>();

		Dispatcher dispatcher = null;

		RoutingPredicate predicate = null;

		ConditionalVisitor<RouteNode> vis = null;

		switch (message.getType()) {
		case CALL: {
			dispatcher = new RequestDispatcher(localRouter, (CallMessage)message);
			predicate = new RoutingPredicate(dispatcher);
			vis = new ConditionalVisitor<>(predicate, (TreeVisitor<RouteNode>) dispatcher);
			break;
		}

		default: {
			dispatcher = new RouteNodeDispatcher(message);
			predicate = new RoutingPredicate(dispatcher);
			vis = new ConditionalVisitor<>(predicate, (TreeVisitor<RouteNode>) dispatcher);
		}
		}

		nav.navigateAndApplyVisitor(routeTree, vis);
	}

}
