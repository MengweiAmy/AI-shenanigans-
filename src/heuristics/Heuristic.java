package heuristics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import atoms.Position;
import searchclient.Node;
import searchclient.SearchClient;

public abstract class Heuristic implements Comparator<Node> {

	public Node initialState;

	public Heuristic(Node initialState) {
		this.initialState = initialState;
	}

	public int compare(Node n1, Node n2) {
		return f(n1) - f(n2);
	}

	public abstract int f(Node n);

	public int h(Node n) {
		int sum = 0;
		int shortest = Integer.MAX_VALUE;
		List<Position> calcBoxes = new ArrayList<Position>(0);
		for (int i = 1; i < SearchClient.MAX_ROW - 1; i++) {
			for (int j = 1; j < SearchClient.MAX_COLUMN - 1; j++) {
				if ('A' <= n.boxes[i][j] && n.boxes[i][j] <= 'Z') {
					if (SearchClient.precomputedGoalH.size() == 1) {
						Byte[][] dist = SearchClient.precomputedGoalH.get(Character.toLowerCase(n.boxes[i][j]));
						int ax = Math.abs(i - n.agentRow);
						int ay = Math.abs(j - n.agentCol);
						if (ax + ay + dist[i][j] < shortest) {
							shortest = ax + ay + dist[i][j];
							sum = ax + ay + dist[i][j];
						}
					} else if (!calcBoxes.contains(new Position(i, j))) { //
						Byte[][] hat = SearchClient.precomputedGoalH.get(Character.toLowerCase(n.boxes[i][j]));
						sum += hat[i][j];
						calcBoxes.add(new Position(i, j));
					}
				}
			}
		}
		return sum;
	}
}