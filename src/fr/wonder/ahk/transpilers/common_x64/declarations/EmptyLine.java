package fr.wonder.ahk.transpilers.common_x64.declarations;

public class EmptyLine implements Declaration {
	
	public static final EmptyLine EMPTY_LINE = new EmptyLine();
	
	private EmptyLine() {}
	
	@Override
	public String toString() {
		return "";
	}
	
}
