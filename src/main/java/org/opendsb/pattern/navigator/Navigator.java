package org.opendsb.pattern.navigator;

import org.opendsb.pattern.composite.Part;
import org.opendsb.pattern.visitor.TreeVisitor;

public interface Navigator<T extends Part<T>> {
	public void navigateAndApplyVisitor(Part<T> host, TreeVisitor<T> visitor);
}
