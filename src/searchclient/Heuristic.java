package searchclient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public abstract class Heuristic implements Comparator<Node> {

	public Node initialState;

	public Heuristic(Node initialState) {
		this.initialState = initialState;
	}

	public int compare(Node n1, Node n2) {
		return f(n1) - f(n2);
	}

	public int h(Node n) {
		int sum = 0;
		int shortest = Integer.MAX_VALUE;
		List<Coord> calcBoxes = new ArrayList<Coord>();
		for (int i = 1; i < SearchClient.MAX_ROW - 1; i++) {
			for (int j = 1; j < SearchClient.MAX_COLUMN - 1; j++) {
				if ('A' <= n.boxes[i][j] && n.boxes[i][j] <= 'Z') {
					if(SearchClient.precomputedGoalH.size() == 1){
						Byte[][] hat = SearchClient.precomputedGoalH.get(Character.toLowerCase(n.boxes[i][j]));
						int ax = Math.abs(i - n.agentRow);
						int ay = Math.abs(j - n.agentCol);
						if(ax+ay+hat[i][j] < shortest){
							shortest = ax+ay+hat[i][j];
							sum = ax+ay+hat[i][j];
						}
					}
					else if (!calcBoxes.contains(new Coord(i, j))) { //
						Byte[][] hat = SearchClient.precomputedGoalH.get(Character.toLowerCase(n.boxes[i][j]));
						sum += hat[i][j];
						calcBoxes.add(new Coord(i, j));
					}

				}
			}
		}
		return sum;
	}

	public abstract int f(Node n);

	public static class Coord {
		public int x, y;

		public Coord(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	public static class AStar extends Heuristic {
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

	public static class WeightedAStar extends Heuristic {
		private int W;

		public WeightedAStar(Node initialState) {
			super(initialState);
			W = 5; // You're welcome to test this out with different values, but
					// for the reporting part you must at least indicate
					// benchmarks for W = 5
		}

		public int f(Node n) {
			return n.g() + W * h(n);
		}

		public String toString() {
			return String.format("WA*(%d) evaluation", W);
		}
	}

	public static class Greedy extends Heuristic {

		public Greedy(Node initialState) {
			super(initialState);
		}

		public int f(Node n) {
			return h(n);
		}

		public String toString() {
			return "Greedy evaluation";
		}
	}
}
