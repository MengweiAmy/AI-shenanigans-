package searchclient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import atoms.Agent;
import heuristics.AStar;
import strategies.Strategy;
import strategies.StrategyBestFirst;

public class Run {

	public static void main(String[] args) throws Exception {
		System.err.println("SearchClient initializing. I am sending this using the error output stream.");
		try {
			SearchClient client = new SearchClient();
			client.init();
			if (args.length > 1)
				SearchClient.TIME = Integer.parseInt(args[1]);
			Strategy strategy = null;
			/* 1. Create solutions for each agent */
			List<LinkedList<Node>> allSolutions = new ArrayList<LinkedList<Node>>();

			for (Agent a : SearchClient.agents) {
				strategy = new StrategyBestFirst(new AStar(a.initialState));
				LinkedList<Node> solution = client.search(strategy, a.initialState);
				if (solution != null) {
					System.err.println("\nSummary for " + strategy);
					System.err.println("Found solution of length " + solution.size());
					System.err.println(strategy.searchStatus());
					allSolutions.add(solution);
				}
			}
			/* 2. Merge simple solutions together */
			int size = 0;
			for (LinkedList<Node> list : allSolutions) {
				if (size < list.size())
					size = list.size();
			}
			String[] test = new String[size];
			for (int m = 0; m < size; m++) {
				for (LinkedList<Node> list : allSolutions) {
					if (m < list.size()) {
						test[m] += list.get(m).action.toString() + ",";
					} else {
						test[m] += "NoOp,";
					}
				}
			}
			for (String tester : test) {
				String newString = tester.replace("null", "[");
				String s = newString.substring(0, newString.length() - 1) + "]";
				System.out.println(s);
				System.err.println(s);
				// Find out if the joint action was succesful or not.
				// String response = serverMessages.readLine();
			}
			/* 3. Use Update function to send solution */
			// Use stderr to print to console
			// while ( client.update() )
			// ;
			//
		} catch (IOException e) {
		}
	}
}