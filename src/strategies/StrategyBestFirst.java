package strategies;

import java.util.PriorityQueue;

import searchclient.Heuristic;
import searchclient.Node;

public class StrategyBestFirst extends Strategy {
	private Heuristic heuristic;
	private PriorityQueue<Node> frontier;
	
	public StrategyBestFirst(Heuristic h) {
		super();
		heuristic = h;
		frontier = new PriorityQueue<Node>(h);
	}

	public Node getAndRemoveLeaf() {
		return frontier.remove();
	}

	public void addToFrontier(Node n) {
		frontier.add(n);
	}

	public int countFrontier() {
		return frontier.size();
	}

	public boolean frontierIsEmpty() {
		return frontier.isEmpty();
	}

	public boolean inFrontier(Node n) {
		return frontier.contains(n);
	}

	public String toString() {
		return "Best-first Search (PriorityQueue) using "
				+ heuristic.toString();
	}
}