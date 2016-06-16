package org.opendsb.routing;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import org.opendsb.pattern.composite.Composite;

public class CompositeRouteNode extends RouteNode implements Composite<RouteNode> {

	private Set<RouteNode> children = new HashSet<>();

	public CompositeRouteNode(String nodeId) {
		super(nodeId);
	}

	@Override
	public Stream<RouteNode> getChildren() {
		return children.stream();
	}

	public boolean addChild(RouteNode child) {
		boolean added = children.add(child);

		if (added) {
			child.setParent(this);
		}

		return added;
	}
}
