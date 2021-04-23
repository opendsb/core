package org.opendsb.pattern.visitor;

public interface Host<T> {
	public void accept(TreeVisitor<T> visitor);
}
