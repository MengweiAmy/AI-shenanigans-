package searchclient;

import heuristics.AStar;

import java.util.LinkedList;

import strategies.Strategy;
import strategies.StrategyBestFirst;
import atoms.Agent;

public class ReplanSolution {
	
	public LinkedList<Node> replan(SearchClient client,Agent agent,Node newPosition) {
		Node node = new Node(null,agent.getId());
		node.action = newPosition.action;
		node.agentCol = newPosition.agentCol;
		node.agentRow = newPosition.agentRow;
		node.boxes = newPosition.boxes;
		node.goals = newPosition.goals;
		node.parent = null;
		
		Strategy strategy = new StrategyBestFirst(new AStar(node));
		LinkedList<Node> solution = client.search(strategy, node);
		return solution;
	}


}
