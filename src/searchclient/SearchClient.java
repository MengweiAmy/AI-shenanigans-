package searchclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import searchclient.Heuristic.AStar;
import strategies.Strategy;
import strategies.StrategyBestFirst;

public class SearchClient {
	public static int MAX_ROW = 0;
	public static int MAX_COLUMN = 0;
	private static int time = 300;
	public static boolean[][] walls;
	public static HashMap<Character, Byte[][]> precomputedGoalH;
	private BufferedReader in;
	private static List<Agent> agents;
	private Map<Character, String> colors;

	public SearchClient(BufferedReader serverMessages) throws IOException {
		precomputedGoalH = new HashMap<Character, Byte[][]>();
		in = new BufferedReader(new InputStreamReader(System.in));
		agents = new ArrayList<Agent>();
		readMap();
	}
	
	public boolean update() throws IOException {
		String jointAction = "[";

		for (int i = 0; i < agents.size() - 1; i++)
			jointAction += agents.get(i).act() + ",";

		jointAction += agents.get(agents.size() - 1).act() + "]";

		// Place message in buffer
		System.out.println(jointAction);
		System.err.println(jointAction);
		// Flush buffer
		System.out.flush();

		// Disregard these for now, but read or the server stalls when its
		// output buffer gets filled!
		String percepts = in.readLine();
		if (percepts == null)
			return false;

		return true;
	}

	private void readMap() throws IOException {
		colors = new HashMap<Character, String>();
		String line, color;

		int row = 0, column = 0;
		ArrayList<String> messages = new ArrayList<String>();

		// Read lines specifying colors
		while ((line = in.readLine()).matches("^[a-z]+:\\s*[0-9A-Z](,\\s*[0-9A-Z])*\\s*$")) {
			line = line.replaceAll("\\s", "");
			color = line.split(":")[0];

			for (String id : line.split(":")[1].split(",")) {
				colors.put(id.charAt(0), color);
			}
		}

		// Read lines specifying level layout
		while (!line.equals("")) {
			messages.add(line);
			for (int i = 0; i < line.length(); i++) {
				char id = line.charAt(i);
				if ('0' <= id && id <= '9') {
					System.err.println(colors.get(id));
					agents.add(new Agent(id, colors.get(id)));
				}
			}
			if (line.length() > column) {
				column = line.length();
			}
			row++;
			line = in.readLine();
		}
		MAX_ROW = row;
		MAX_COLUMN = column;

		for (Agent agent : agents) {
			Node node = new Node(null);
			agent.SetInitialState(node);
		}
		walls = new boolean[SearchClient.MAX_ROW][SearchClient.MAX_COLUMN];

		int levelLines = 0;

		for (String str : messages) {
			for (int i = 0; i < str.length(); i++) {
				char chr = str.charAt(i);
				if ('+' == chr) { // Walls
					SearchClient.walls[levelLines][i] = true;
				} else if ('0' <= chr && chr <= '9') { // Agents
					for (Agent agent : agents) {
						if (agent.getCol() == Utils.determineColor(colors.get(chr))) {
							agent.initialState.agentRow = levelLines;
							agent.initialState.agentCol = i;
						}
					}
				}
			}
			levelLines++;
		}
		levelLines = 0;
		for (String str : messages) {
			for (int i = 0; i < str.length(); i++) {
				char chr = str.charAt(i);
				if ('A' <= chr && chr <= 'Z') { // Boxes
					for (Agent agent : agents) {
						if (agent.getCol() == Utils.determineColor(colors.get(chr))) {
							agent.initialState.boxes[levelLines][i] = chr;
						}
					}
				}
			}
			levelLines++;
		}
		levelLines = 0;
		for (String str : messages) {
			for (int n = 0; n < str.length(); n++) {
				char chr = str.charAt(n);
				if ('a' <= chr && chr <= 'z') { // Goal cells
					for (Agent agent : agents) {
						for (int k = 1; k < MAX_ROW - 1; k++) {
							for (int l = 1; l < MAX_COLUMN - 1; l++) {
								if (Character.toLowerCase(agent.initialState.boxes[k][l]) == chr) {
									agent.initialState.goals[levelLines][n] = chr;
								}
							}
						}
					}
				}
			}
			levelLines++;
		}

		/* Needs to be modified such that it calculates for each agent */
		for (Agent agent : agents) {
			for (int i = 1; i < MAX_ROW - 1; i++) {
				for (int j = 1; j < MAX_COLUMN - 1; j++) {
					if ('a' <= agent.initialState.goals[i][j] && agent.initialState.goals[i][j] <= 'z') {
						precomputedGoalH.put(agent.initialState.goals[i][j], Utils.calculateDistanceValues(i, j, agent.initialState.goals[i][j], MAX_ROW, MAX_COLUMN));
					}
				}
			}
		}
	}

	public LinkedList<Node> Search(Strategy strategy, Node initialState) throws IOException {
		System.err.format("Search starting with strategy %s\n", strategy);
		strategy.addToFrontier(initialState);
		int iterations = 0;
		while (true) {
			if (iterations % 200 == 0) {
				System.err.println(strategy.searchStatus());
			}
			if (Memory.shouldEnd()) {
				System.err.format("Memory limit almost reached, terminating search %s\n", Memory.stringRep());
				return null;
			}
			if (strategy.timeSpent() > time) { // Minutes timeout
				System.err.format("Time limit reached, terminating search %s\n", Memory.stringRep());
				return null;
			}

			if (strategy.frontierIsEmpty()) {
				return null;
			}

			Node leafNode = strategy.getAndRemoveLeaf();

			if (leafNode.isGoalState()) {
				return leafNode.extractPlan();
			}

			strategy.addToExplored(leafNode);
			for (Node n : leafNode.getExpandedNodes()) { // The list of expanded
				// nodes is shuffled
				// randomly; see
				// Node.java
				if (!strategy.isExplored(n) && !strategy.inFrontier(n)) {
					strategy.addToFrontier(n);
				}
			}
			iterations++;
		}
	}

	public static void main(String[] args) throws Exception {
		BufferedReader serverMessages = new BufferedReader(new InputStreamReader(System.in));
		System.err.println("SearchClient initializing. I am sending this using the error output stream.");

		try {
			SearchClient client = new SearchClient(serverMessages);

			if (args.length > 1) {
				time = Integer.parseInt(args[1]);
			}

			Strategy strategy = null;
			/* 1. Create solutions for each agent */
			List<LinkedList<Node>> allSolutions = new ArrayList<LinkedList<Node>>();
			for (Agent agent : agents) {
				strategy = new StrategyBestFirst(new AStar(agent.initialState));
				LinkedList<Node> solution = client.Search(strategy, agent.initialState);
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
//				String response = serverMessages.readLine();
			}

			/* 3. Use Update function to send solution */
			// Use stderr to print to console
			// while ( client.update() )
			// ;
			//
		} catch (IOException e) {
			// Got nowhere to write to probably
		}
	}
	
	public static void error(String msg) throws Exception {
		throw new Exception("GSCError: " + msg);
	}
}
