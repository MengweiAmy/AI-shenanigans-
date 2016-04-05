package searchclient;

import atoms.Color;

public class Utils {
	
	public static Byte[][] calculateDistanceValues(int x, int y, char id, int MAX_ROW, int MAX_COLUMN) {
		Byte[][] result = new Byte[MAX_ROW][MAX_COLUMN];
		result[x][y] = 0;
		for (int i = 1; i < MAX_ROW - 1; i++) {
			for (int j = 1; j < MAX_COLUMN - 1; j++) {
				result[i][j] = (byte) (Math.abs(i - x) + Math.abs(j - y));
			}
		}
		return result;
	}
	
	public static Color determineColor(String color) {
		if(color == null)
			return Color.NONE;
		color = color.toLowerCase();
		switch (color) {
		case "green":
			return Color.GREEN;
		case "blue":
			return Color.BLUE;
		case "red":
			return Color.RED;
		case "cyan":
			return Color.CYAN;
		case "magenta":
			return Color.MAGENTA;
		case "orange":
			return Color.ORANGE;
		case "pink":
			return Color.PINK;
		case "yellow":
			return Color.YELLOW;
		default:
			return Color.NONE;
		}
	}
}
