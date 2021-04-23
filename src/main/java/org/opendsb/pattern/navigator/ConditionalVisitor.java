package org.opendsb.pattern.navigator;

import java.util.function.Predicate;

import org.apache.log4j.Logger;
import org.opendsb.pattern.visitor.TreeVisitor;

public class ConditionalVisitor<T> implements TreeVisitor<T> {

	private static Logger logger = Logger.getLogger(ConditionalVisitor.class);

	private Predicate<T> predicate;

	private TreeVisitor<T> visitor;

	public ConditionalVisitor(Predicate<T> predicate, TreeVisitor<T> visitor) {
		super();
		this.predicate = predicate;
		this.visitor = visitor;
	}

	@Override
	public void visit(T host) {
		logger.trace("Visiting '" + host.toString() + "'.");
		if (predicate.test(host)) {
			visitor.visit(host);
			logger.trace("Host '" + host.toString() + "' aprovado.");
		}
	}

	@Override
	public void increaseDepth() {
		visitor.increaseDepth();
	}

	@Override
	public void decreaseDepth() {
		visitor.decreaseDepth();
	}
}
