package heuristics;

import searchclient.Node;

public class AStar extends Heuristic {
	public AStar(Node initialState) {
		super(initialState);
	}

	public int f(Node n) {
		return n.g() + h(n);
	}

	public String toString() {
		return "A* evaluation";
	}
}