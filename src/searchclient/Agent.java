package searchclient;


import sampleclients.Command;

public class Agent {
	private int id;
	private Color col;
	public Node initialState = null;
	
	public Agent(int id, String color) {
		this.id = id;
		this.col = Utils.determineColor(color);
	}

	public String act() {
		return Command.every[1].toString();
	}

	public void SetInitialState(Node initial) {
		initialState = initial;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public Color getCol() {
		return col;
	}

	public void setCol(Color col) {
		this.col = col;
	}

	public Node getInitialState() {
		return initialState;
	}

	public void setInitialState(Node initialState) {
		this.initialState = initialState;
	}
}
