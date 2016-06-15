package org.opendsb.routing;

import org.dsb.pattern.navigator.ConditionalVisitor;
import org.dsb.pattern.navigator.Navigator;
import org.dsb.pattern.navigator.TreeTopDownNavigator;
import org.dsb.pattern.visitor.TreeVisitor;
import org.opendsb.messaging.Message;
import org.opendsb.routing.remote.RemoteRouter;

public class RoutingTask implements Runnable {

	private CompositeRouteNode routeTree;
	
	private Message message;
	
	private RemoteRouter remoteRouter = null;
	
	public RoutingTask(CompositeRouteNode routeTree, Message message) {
		super();
		this.message = message;
		this.routeTree = routeTree;
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
		
		switch(message.getType()) {
			case CALL: {
				dispatcher = new RequestDispatcher(message);
				predicate = new RoutingPredicate(dispatcher);
				vis = new ConditionalVisitor<>(predicate, (TreeVisitor<RouteNode>)dispatcher);
				break;
			}
			
			default: {
				dispatcher = new RouteNodeDispatcher(message);
				predicate = new RoutingPredicate(dispatcher);
				vis = new ConditionalVisitor<>(predicate, (TreeVisitor<RouteNode>)dispatcher);
			}
		}
		
		nav.navigateAndApplyVisitor(routeTree, vis);
	}

}
