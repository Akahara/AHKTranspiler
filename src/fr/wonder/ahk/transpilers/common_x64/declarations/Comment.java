package fr.wonder.ahk.transpilers.common_x64.declarations;

public class Comment implements Declaration {
	
	private final int indentation;
	public final String text;
	
	public Comment(String text, int indentation) {
		this.text = text;
		this.indentation = indentation;
	}
	
	public Comment(String text) {
		this(text, 0);
	}
	
	@Override
	public String toString() {
		return " ".repeat(indentation) + "; " + text;
	}

}
