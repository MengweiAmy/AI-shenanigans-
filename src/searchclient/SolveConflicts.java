package searchclient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import atoms.Agent;
import atoms.Box;
import atoms.Position;
import atoms.World;

public class SolveConflicts {
	
	private Agent conflictAgen;

	private SearchClient client;
	
	private boolean isSenderMove = false;
	
	public SolveConflicts (SearchClient client) {
		this.client = client;
	}
	
	/**
	 * Check if the next action of the agent is valid
	 * Only check the agent conflicts
	 * Miss the boxes confilcts(used for solve MAsimple2.lvl for now)
	 * @param node
	 * @param solutionMap
	 * @param agents
	 * @param agent
	 * @return
	 */
	public boolean checkAction(Node node,Agent agent) {
		
		int nodeCol = node.agentCol;
		int nodeRow = node.agentRow;
		
		int agenCo = agent.getPosition().getY();
		int agenRo = agent.getPosition().getX();
		
		for(Integer aid: World.getInstance().getAgents().keySet()) {
			Agent a = World.getInstance().getAgents().get(aid);
			if(a.getId() != agent.getId()) {
				LinkedList<Node> solutionForAgentX = World.getInstance().getSolutionMap().get(a.getId());
				if(solutionForAgentX.size() >0 ) {	
					Node next = solutionForAgentX.peek();
					int nexCol = next.agentCol;
					int nexRow  = next.agentRow;
					//||(next.agentCol == agenCo && next.agentRow == agenRo)
					if(next.agentCol == nodeCol && next.agentRow == nodeRow) {
						if(a.getPriority() > agent.getPriority()) {
							conflictAgen = agent;
						}else {
							conflictAgen = a;
						}
						return false;
					}
				}else {
					if(!isCellFree(nodeRow,nodeCol,agent)) {
						return false;
					}
					return true;
				}
			}
		}		
		return true;
	}
	
	/**
	 * Check the current position of the agent and the next step of the agent
	 * if they are neighbour, then canmove the agent
	 * otherwise need to replan(//TODO to combine with the online replanning)
	 * @param node
	 * @param agent
	 * @return
	 */
	public boolean canMove(Node node,Agent agent) {
		//If the node is not next the agent, then the current agent cannot move
		int col = node.agentCol;
		int row = node.agentRow;
		
		int positionCol = agent.getPosition().getY();
		int positionRow = agent.getPosition().getX();
		
		if(row == positionRow) {
			if(col != positionCol -1 && col != positionCol +1) {
				return false;
			}
		}else if(col == positionCol) {
			if(row != positionRow-1 && row != positionRow +1) {
				return false;
			}
		}else {
			return false;
		}
		return true;
	}
	
	/**
	 * Should have other method to decide which action request should be sent
	 * The current idea is:
	 * First, the sender check whether it can move to up or down,if it can, then move(S) or move(E), and after this, 
	 * 	should call the replan function(need to be implemented) to make a new plan to achieve the goal.
	 * 
	 * Then, if the sender cannot move to south or north, then the receiver should make the move.
	 * 	The same logic that check whether can move to south or north to avoid the conflicts
	 *  if cannot move to south or north, then go back(as he level MAsimple2.lvl shows)
	 *  
	 *  #All the actions taken,should replan the plan for the agents.(need to combine with the online replanning part BDI)
	 * @param sender
	 * @param receiver
	 * @param solutionMap
	 * @return
	 */
	public List<LinkedList<Node>> communicate(Agent sender, Agent receiver,boolean isFinalCheck) {
		//boolean isSenderMove = false;
		boolean isReceMove = false;
		Node newAc = couldMoveAgent(sender);
		if(newAc != null) {
			isSenderMove = true;
		}
		
		if(!isSenderMove) {
			newAc = couldMoveAgent(receiver);
			if(newAc != null) {
				isReceMove = true;
			}
		}
		
		if(!isSenderMove && !isReceMove) {
			newAc = requestMove(receiver);
		}
		

		ReplanSolution replan = new ReplanSolution();
		LinkedList<Node> newSolu = new LinkedList<Node>();
		if(isSenderMove) {
			if(!isFinalCheck) {
				newSolu = replan.replan(client, sender, newAc);
			}
			System.err.println("had confilcts, sender move.....");
			newSolu.add(0, newAc);
			World.getInstance().getSolutionMap().put(sender.getId(), newSolu);
		}else {
			newSolu = replan.replan(client, receiver, newAc);
			System.err.println("had confilcts, receiver move.....");
			newSolu.add(0, newAc);
			World.getInstance().getSolutionMap().put(receiver.getId(), newSolu);
		}		
		
		List<LinkedList<Node>> allSolutions = updateSolution(World.getInstance().getSolutionMap());
		return allSolutions;
	}
	
	/**
	 * Used for one agent already achieved the goals
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

	
	private Node requestMove(Agent receiver) {
		int receCol = receiver.getPosition().getY();
		int receRow = receiver.getPosition().getX();
		//Node receNo = World.getInstance().getSolutionMap().get(receiver.getId()).peek();
		String content = "";
		
		Node newAction = null;
		if(isCellFree(receRow-1,receCol,receiver)) {
			content = "N";
			Command co = new Command(convertToDir(content));
			newAction = createNewAction(co,receRow-1,receCol,receiver);
		}else if(isCellFree(receRow+1,receCol,receiver)) {
			content = "S";
			Command co = new Command(convertToDir(content));
			newAction = createNewAction(co,receRow+1,receCol,receiver);
		}else if(isCellFree(receRow,receCol-1,receiver)){
			content = "W";
			Command co = new Command(convertToDir(content));
			newAction = createNewAction(co,receRow,receCol-1,receiver);
		}else if(isCellFree(receRow, receCol+1,receiver)) {
			content = "E";
			Command co = new Command(convertToDir(content));
			newAction = createNewAction(co,receRow,receCol+1,receiver);
		}
		return newAction;
	}
	
	public Node couldMoveAgent(Agent agent) {
		/**
		 * Just check N and S for MAsimple2
		 */
		Node newAct = null;
		
		//Node senderNo = World.getInstance().getSolutionMap().get(agent.getId()).peek();
		
		int agentCol = agent.getPosition().getY();
		int agentRow = agent.getPosition().getX();
		
		if(isCellFree(agentRow+1,agentCol,agent)) {
			Command c = new Command(Command.dir.S);
			newAct = createNewAction(c,agentRow+1,agentCol,agent);
		}else if(isCellFree(agentRow-1,agentCol,agent)) {
			Command c = new Command(Command.dir.N);
			newAct = createNewAction(c,agentRow-1,agentCol,agent);
		}else if(isCellFree(agentRow,agentCol-1,agent)) {
			Command c = new Command(Command.dir.W);
			newAct = createNewAction(c,agentRow,agentCol-1,agent);
		}else if(isCellFree(agentRow,agentCol+1,agent)) {
			Command c = new Command(Command.dir.E);
			newAct = createNewAction(c,agentRow,agentCol+1,agent);
		}
		return newAct;
	}
	
	private List<LinkedList<Node>> updateSolution(Map<Integer,LinkedList<Node>> solutionMap) {
		List<LinkedList<Node>> allSolutions = new ArrayList<LinkedList<Node>>();
		for(Integer id:solutionMap.keySet()) {
			Agent agent = World.getInstance().getAgents().get(id);
			LinkedList<Node> list = solutionMap.get(agent.getId());
			allSolutions.add(list);
		}
		return allSolutions;
	}
	
	public boolean checkBoxes(int row, int col, Agent agen) {
		for (Integer bId : agen.initialState.boxes.keySet()) {
			Box b = World.getInstance().getBoxes().get(bId);
			if (b.getPosition().equals(new Position(row,col))) {
				return true;
			}
		}
		return true;
	}
	
	/**
	 * Check whether the agent can move to the positon(row,col)
	 * based on the logic that : check is it a wall on this position or a movable box of this agent
	 * OR
	 * is there a box for any other agent, otherwise this cell if free for the agent to move to
	 * @param row
	 * @param col
	 * @param agent
	 * @return
	 */
	public boolean isCellFree(int row, int col,Agent agent) {
		for(Integer aid:World.getInstance().getAgents().keySet()) {
			Agent agen = World.getInstance().getAgents().get(aid);
			if(agen.getId() != agent.getId()) {
				LinkedList<Node> solu = World.getInstance().getSolutionMap().get(agen.getId());
				if(solu.size() > 0) {
					Node next = solu.peek();
					int nextcol = next.agentCol;
					int nextrow = next.agentRow;
					if(nextcol == col && nextrow == row) {
						//If current position is the next position of other agents, then could not move
						return false;
					}
				}
				if(agen.getPosition().getY() == col && agen.getPosition().getX() == row) {
					//if current position is the position of other agent
					return false;
				}else if(!checkBoxes(row,col,agen)){
					//If current position is the box of other agent, then could not move
					return false;
				}
			}
			
		}
		return checkBoxes(row,col,agent) &&  !World.getInstance().getWalls().contains(new Position(row, col));
	}
	
	/**
	 * Create a new action to add 
	 * @param c
	 * @param x
	 * @param y
	 * @param oldN
	 * @return
	 */
	private Node createNewAction(Command c, int x, int y, Agent agent) {
		Map<Integer,Box> nodeBox= new HashMap<Integer,Box>();
		for(Integer intB:agent.initialState.boxes.keySet()) {
			Box b = World.getInstance().getBoxes().get(intB);
			nodeBox.put(intB, b);
		}
		Node node = new Node(null,agent.getId());
		node.action = c;
		node.agentRow = x;
		node.agentCol = y;
		node.boxes= nodeBox;
		node.goals = agent.initialState.goals;
		return node;
	}
	
	public Agent getConflictAgent() {
		return conflictAgen;
	}
	
	public Command.dir convertToDir(String content) {
		switch (content) {
		case "N":
			return Command.dir.N;
		case "W":
			return Command.dir.W;
		case "E":
			return Command.dir.E;
		case "S":
			return Command.dir.S;
		default:
			return Command.dir.E;
		}
	}
	
	public String changeColDir(Command.dir dir) {
		return null;
	}

	public boolean isSenderMove() {
		return isSenderMove;
	}

}
