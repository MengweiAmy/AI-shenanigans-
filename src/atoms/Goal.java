package atoms;

public class Goal {
	private Position position;
	private char letter;
	private Color color;
	
	public Goal(Position position, char letter, Color color) {
		this.position = position;
		this.letter = letter;
		this.color = color;
	}
	
	public Goal(int x, int y, char letter, Color color) {
		this(new Position(x, y), letter, color);
	}
	
	public Position getPosition() {
		return position;
	}
	public void setPosition(Position position) {
		this.position = position;
	}
	public char getLetter() {
		return letter;
	}
	public void setLetter(char letter) {
		this.letter = letter;
	}
	public Color getColor() {
		return color;
	}
	public void setColor(Color color) {
		this.color = color;
	}
}
