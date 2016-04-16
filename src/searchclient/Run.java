package searchclient;

import heuristics.AStar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import strategies.Strategy;
import strategies.StrategyBestFirst;
import atoms.Agent;
import atoms.Box;
import atoms.Goal;
import atoms.Position;
import atoms.World;

public class Run {
	
	static HashMap<Integer,LinkedList<Node>> soluMap = new HashMap<Integer,LinkedList<Node>>();
	
	private static SearchClient client;
	
	public static void main(String[] args) throws Exception {
		System.err.println("SearchClient initializing. I am sending this using the error output stream.");
		try {
			client = new SearchClient();
			client.init();
			if (args.length > 1)
				SearchClient.TIME = Integer.parseInt(args[1]);
			Strategy strategy = null;
			/* 1. Create solutions for each agent */
			List<LinkedList<Node>> allSolutions = new ArrayList<LinkedList<Node>>();
			
			for (Integer id : World.getInstance().getAgents().keySet()) {
				Agent a = World.getInstance().getAgents().get(id);
				strategy = new StrategyBestFirst(new AStar(a.initialState));
				LinkedList<Node> solution = client.search(strategy, a.initialState);
				if (solution != null) {
					System.err.println("\nSummary for " + strategy);
					System.err.println("Found solution of length " + solution.size());
					System.err.println(strategy.searchStatus());
					soluMap.put(a.getId(), solution);
					
					allSolutions.add(solution);
				} else {
					System.err.println("!!!!!!");
				}
			}
			
			World.getInstance().setSolutionMap(soluMap);
			runSolutions(allSolutions);
		} catch (IOException e) { 
			System.err.println(e.getMessage());
		}
	}
	
	private static void runSolutions(List<LinkedList<Node>> allSolutions) {
		/* 2. Merge simple solutions together */
		int size = 0;
		SolveConflicts solve = new SolveConflicts(client);
		
		for (LinkedList<Node> solution : allSolutions) {
			if (size < solution.size())
				size = solution.size();
		}
		
		for (int m = 0; m < size; m++) {
			StringBuilder sb = new StringBuilder();
			sb.append("[");
			int i = 0;
			
			List<LinkedList<Node>> newSol = new ArrayList<LinkedList<Node>>();
			boolean isReplan = false;
			
			boolean achieved = checkAgentGoals();
			if(achieved) {
				break;
			}else {
				for (LinkedList<Node> solution : allSolutions) {
					
					
					// set agent position
					if(solution.size() == 0) {
						isReplan  = solveAchievedAgentConflict(newSol,sb,i,solve);
						if (i < allSolutions.size() - 1){
							i++;
						}
						
						continue;
					}
					Node n = solution.peek(); 
					Agent agent = World.getInstance().getAgents().get(n.agentId);
						
					//first check the next node from the solution whether can move or not
					boolean isCanMove = solve.canMove(n, agent);
					if(isCanMove){
						boolean isNextValid = solve.checkAction(n, agent);
						if(isNextValid) {
							executeAction(sb,allSolutions,i);
						}else {
							/**
							 * Check which agent should be moved away
							 */
							Agent receiver = solve.getConflictAgent();
							newSol = solve.communicate(agent, receiver,false);
							executeAction(sb,newSol,i);
							isReplan = true;
							//sb.append("NoOp");
						}
					}else {
						/**
						 * If next step can not move, replan the current agent
						 */
						newSol = replannedSolutions(agent,n);
						isReplan = true;
						sb.append("NoOp");
					}
					if (i < allSolutions.size() - 1)
						sb.append(", ");
					i++;
				}
			}
			sb.append("]");
			System.out.println(sb.toString());
			//System.err.println(sb.toString());
			if(isReplan) {
				runSolutions(newSol);
			}
		}
	}
	
	private static boolean solveAchievedAgentConflict(List<LinkedList<Node>> newSol,StringBuilder sb, int index,SolveConflicts solve) {//For MAsimple5
		Agent agent = null;
		boolean isReplan = false;
		for(Integer inta:World.getInstance().getAgents().keySet()) {
			LinkedList<Node> node = World.getInstance().getSolutionMap().get(inta);
			if(node.size() == 0) {
				agent = World.getInstance().getAgents().get(inta);
			}
		}
		Agent conl = checkRouteConflicts(agent);
		if(conl != null) {
			newSol = solve.communicate(agent, conl,true);
			if(solve.isSenderMove()) {
				executeAction(sb,newSol,index);
			}else {
				sb.append("NoOp");
				sb.append(", ");
				index++;
			}
			//executeAction(sb,newSol,i);
			isReplan = true;
		}else {
			sb.append("NoOp");
			sb.append(", ");
			index++;
		}
		return isReplan;
			
	}
	/**
	 * Check whether the achieved agent has occured the route of other agent
	 * Then try to find the way out
	 * @param aget
	 * @return
	 */
	public static Agent checkRouteConflicts(Agent aget) {
		for(Integer inda: World.getInstance().getAgents().keySet()) {
			Agent agent = World.getInstance().getAgents().get(inda);
			if(agent.getId() != aget.getId()) {
				LinkedList<Node> nodels = World.getInstance().getSolutionMap().get(agent.getId());
				for(Node node:nodels) {
					int row = node.agentRow;
					int col = node.agentCol;
					if(aget.getPosition().equals(new Position(row,col))) {
						return agent;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Execute the first action of the solution
	 * @param sb
	 * @param solution
	 * @param agent
	 * @return
	 */
	private static LinkedList<Node> executeAction(StringBuilder sb,List<LinkedList<Node>> allsolutions,int agentId) {
		LinkedList<Node> solution = allsolutions.get(agentId);
		Node node = solution.pop();
		sb.append(node.action.toString());
		//System.err.println(sb.toString());
		Agent agent = World.getInstance().getAgents().get(agentId);
		World.getInstance().getSolutionMap().put(agent.getId(), solution);
		agent.setPosition(new Position(node.agentRow, node.agentCol));
		World.getInstance().getAgents().put(agent.getId(), agent);
		
		World world = World.getInstance();
		
		for(Integer bId : node.boxes.keySet()) {
			World.getInstance().getBoxes().put(bId, node.boxes.get(bId));
		}
		return solution;
	}
	
	
	/**
	 * After solved each conflict should replan the solutions
	 * @param world
	 * @param agent
	 * @param n
	 * @return
	 */
	private static List<LinkedList<Node>> replannedSolutions(Agent agent, Node n) {
		ReplanSolution replan = new ReplanSolution();
		LinkedList<Node> reSolu = replan.replan(client,agent,n);
		World.getInstance().getSolutionMap().put(agent.getId(), reSolu);
		List<LinkedList<Node>> newSol = updateSolution(World.getInstance().getSolutionMap());
		return newSol;
	}
	
	private static boolean checkAgentGoals() {
		int agentSize = World.getInstance().getAgents().size();
		int achSize = 0;
		for(Integer inta:World.getInstance().getAgents().keySet()) {
			Agent agent = World.getInstance().getAgents().get(inta);
			
			Map<Integer, Goal> goals = agent.initialState.goals;
			int goalSize = goals.size();
			
			int size = 0;
			for(Integer intg: goals.keySet()) {
				Goal goal = goals.get(intg);
				Map<Integer, Box> boxes = World.getInstance().getBoxes();
				for(Integer intb:boxes.keySet()) {
					Box box = boxes.get(intb);
					if(goal.getPosition().equals(box.getPosition()) &&
							goal.getLetter() == Character.toLowerCase(box.getLetter())) {
						size++;
					}
				}
			}
			
			if(goalSize == size) {
				achSize++;
			}
		}
		if(agentSize == achSize) {
			return true;
		}else {
			return false;
		}
	}
	
	private static List<LinkedList<Node>> updateSolution(Map<Integer,LinkedList<Node>> solutionMap) {
		List<LinkedList<Node>> allSolutions = new ArrayList<LinkedList<Node>>();
		for(Integer id:solutionMap.keySet()) {
			Agent agent = World.getInstance().getAgents().get(id);
			LinkedList<Node> list = solutionMap.get(agent.getId());
			allSolutions.add(list);
		}
		return allSolutions;
	}
}