package org.opendsb.pattern.visitor;

public interface Visitor<T> {
	public void visit(T host);
}
