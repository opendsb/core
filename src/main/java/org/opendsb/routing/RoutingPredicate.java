package org.opendsb.routing;

import java.util.function.Predicate;

public class RoutingPredicate implements Predicate<RouteNode> {

	private Dispatcher dispatcher;

	public RoutingPredicate(Dispatcher dispatcher) {
		super();
		this.dispatcher = dispatcher;
	}

	@Override
	public boolean test(RouteNode node) {
		return node.getNodeId().equals(dispatcher.getCurrentNode());
	}
}
