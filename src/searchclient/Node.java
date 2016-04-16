package searchclient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;

import atoms.Box;
import atoms.Goal;
import atoms.Position;
import atoms.World;
import searchclient.Command.dir;
import searchclient.Command.type;

public class Node {

	private static Random rnd = new Random(1);

	public int agentRow;
	public int agentCol;
	
	public Map<Integer, Box> boxes;
	public Map<Integer, Goal> goals;

	public Node parent;
	public Command action;

	private int g;
	public int agentId;

	public Node(Node parent, int agentId) {
		this.parent = parent;
		this.agentId = agentId;
		if (parent == null) {
			g = 0;
		} else {
			g = parent.g() + 1;
		}
	}

	public int g() {
		return g;
	}

	public boolean isInitialState() {
		return this.parent == null;
	}
	
	public boolean isGoalState() {
		boolean result = false;
		for (Integer goalId : goals.keySet()) {
			for (Integer boxId : boxes.keySet()) {
				Goal goal = goals.get(goalId);
				Box box = boxes.get(boxId);
				if (goal.getLetter() == Character.toLowerCase(box.getLetter())) {
					if (goal.getPosition().equals(box.getPosition())) {
						result = true;
						break;
					} else
						result = false;
				}
			}
			if (!result) {
				return false;
			}
		}
		return result;
	}

	public ArrayList<Node> getExpandedNodes() {
		ArrayList<Node> expandedNodes = new ArrayList<Node>(Command.every.length);
		for (Command c : Command.every) {
			// Determine applicability of action
			int newAgentRow = this.agentRow + dirToRowChange(c.dir1);
			int newAgentCol = this.agentCol + dirToColChange(c.dir1);
			if (c.actType == type.Move) {
				// Check if there's a wall or box on the cell to which the agent
				// is moving
				if (cellIsFree(newAgentRow, newAgentCol)) {
					Node n = this.childNode();
					n.action = c;
					n.agentRow = newAgentRow;
					n.agentCol = newAgentCol;
					expandedNodes.add(n);
				}
			} else if (c.actType == type.Push) {
				// Make sure that there's actually a box to move
				if (boxAt(newAgentRow, newAgentCol)) {
					int newBoxRow = newAgentRow + dirToRowChange(c.dir2);
					int newBoxCol = newAgentCol + dirToColChange(c.dir2);
					// .. and that new cell of box is free
					if (cellIsFree(newBoxRow, newBoxCol)) {
						Node n = this.childNode();
						n.action = c;
						n.agentRow = newAgentRow;
						n.agentCol = newAgentCol;
						Box foundBox = null;
						for (Integer bId : boxes.keySet()) {
							Box b = boxes.get(bId);
							if (b.getPosition().equals(new Position(newAgentRow, newAgentCol))) {
								foundBox = b;
								break;
							}
						}
						n.boxes.put(foundBox.getId(), new Box(foundBox.getId(), new Position(newBoxRow, newBoxCol), foundBox.getLetter(), foundBox.getColor()));
						expandedNodes.add(n);
					}
				}
			} else if (c.actType == type.Pull) {
				// Cell is free where agent is going
				if (cellIsFree(newAgentRow, newAgentCol)) {
					int boxRow = agentRow + dirToRowChange(c.dir2);
					int boxCol = agentCol + dirToColChange(c.dir2);
					// .. and there's a box in "dir2" of the agent
					if (boxAt(boxRow, boxCol)) {
						Node n = this.childNode();
						n.action = c;
						n.agentRow = newAgentRow;
						n.agentCol = newAgentCol;
						Box foundBox = null;
						for (Integer bId : boxes.keySet()) {
							Box b = boxes.get(bId);
							if (b.getPosition().equals(new Position(boxRow, boxCol))) {
								b.setPosition(new Position(boxRow, boxCol));
								foundBox = b;
								break;
							}
						}
						n.boxes.put(foundBox.getId(), new Box(foundBox.getId(), new Position(agentRow, agentCol), foundBox.getLetter(), foundBox.getColor()));
						expandedNodes.add(n);
					}
				}
			}
		}
		Collections.shuffle(expandedNodes, rnd);
		return expandedNodes;
	}

	private boolean cellIsFree(int row, int col) {
		Position pos = new Position(row, col);
		for (Integer bId : boxes.keySet()) {
			Box b = boxes.get(bId);
			if (b.getPosition().equals(pos))
				return false;
		}
		return (!World.getInstance().getWalls().contains(pos));
	}

	private boolean boxAt(int row, int col) {
		for (Integer bId : boxes.keySet()) {
			Box b = boxes.get(bId);
			if (b.getPosition().equals(new Position(row, col)))
				return true;
		}
		return false;
	}

	private int dirToRowChange(dir d) {
		return (d == dir.S ? 1 : (d == dir.N ? -1 : 0)); // South is down one
															// row (1), north is
															// up one row (-1)
	}

	private int dirToColChange(dir d) {
		return (d == dir.E ? 1 : (d == dir.W ? -1 : 0)); // East is left one
															// column (1), west
															// is right one
															// column (-1)
	}

	private Node childNode() {
		Node copy = new Node(this, this.agentId);
		copy.boxes = new HashMap<Integer, Box>(this.boxes);
		copy.goals = this.goals;
		copy.agentCol = this.agentCol;
		copy.agentRow = this.agentRow;
		return copy;
	}

	public LinkedList<Node> extractPlan(Node node) {
		LinkedList<Node> plan = new LinkedList<Node>();
		Node n = this;
		if(node.parent == null) {
			while (!n.isInitialState()) {
				plan.addFirst(n);
				n = n.parent;
			}
		}else {
			while(!node.equals(n)) {
				plan.addFirst(n);
				n = n.parent;
			}
		}
		
		return plan;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + agentCol;
		result = prime * result + agentRow;
		result = prime * result + boxes.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Node other = (Node) obj;
		if (agentCol != other.agentCol)
			return false;
		if (agentRow != other.agentRow)
			return false;
		if(!boxes.equals(other.boxes))
			return false;
		return true;
	}

	public String toString() {
		StringBuilder s = new StringBuilder();
		for (int row = 0; row < SearchClient.MAX_ROW; row++) {
			if (!World.getInstance().getWalls().contains(new Position(row, 0))) {
				break;
			}
			for (int col = 0; col < SearchClient.MAX_COLUMN; col++) {
				boolean skip = false;
				Position pos = new Position(row, col);
				for (Integer bId : boxes.keySet()) {
					Box b = boxes.get(bId);
					if (b.getPosition().equals(pos)) {
						s.append(b.getLetter());
						skip = true;
						break;
					}
				}
				if (skip)
					continue;
				for (Integer gId : goals.keySet()) {
					Goal g = goals.get(gId);
					if (g.getPosition().equals(pos)) {
						s.append(g.getLetter());
						skip = true;
						break;
					}
				}
				if (skip)
					continue;
				if (World.getInstance().getWalls().contains(pos)) {
					s.append("+");
				} else if (row == this.agentRow && col == this.agentCol) {
					s.append(agentId);
				} else {
					s.append(" ");
				}
			}
			s.append("\n");
		}
		return s.toString();
	}
}