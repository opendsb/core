package org.opendsb.pattern.navigator;

import org.opendsb.pattern.composite.Composite;
import org.opendsb.pattern.composite.Part;
import org.opendsb.pattern.visitor.TreeVisitor;

public class TreeTopDownNavigator<T extends Part<T>> implements Navigator<T> {

	@Override
	public void navigateAndApplyVisitor(Part<T> host, TreeVisitor<T> visitor) {
		host.accept(visitor);
		if (host instanceof Composite) {
			visitor.increaseDepth();
			((Composite<T>) host).getChildren().forEach(part -> navigateAndApplyVisitor(part, visitor));
			visitor.decreaseDepth();
		}
	}
}
