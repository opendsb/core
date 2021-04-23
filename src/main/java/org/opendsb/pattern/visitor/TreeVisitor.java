package org.opendsb.pattern.visitor;

public interface TreeVisitor<T> extends Visitor<T> {
	public void increaseDepth();

	public void decreaseDepth();
}
