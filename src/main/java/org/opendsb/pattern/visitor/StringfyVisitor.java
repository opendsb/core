package org.opendsb.pattern.visitor;

public class StringfyVisitor<T> implements TreeVisitor<T> {

	private int depth = 0;

	@Override
	public void visit(T host) {
		System.out.println(generateSpaces(depth) + host.toString());
	}

	private String generateSpaces(int numSpaces) {
		String spaces = "";

		for (int i = 0; i < numSpaces; i++) {
			spaces += "    ";
		}

		return spaces;
	}

	@Override
	public void increaseDepth() {
		depth++;
	}

	@Override
	public void decreaseDepth() {
		depth--;
	}
}
