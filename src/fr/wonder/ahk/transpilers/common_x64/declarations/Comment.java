package fr.wonder.ahk.transpilers.common_x64.declarations;

public class Comment implements Declaration {
	
	public final String text;
	
	public Comment(String text) {
		this.text = text;
	}
	
	@Override
	public String toString() {
		return "; " + text;
	}

}
