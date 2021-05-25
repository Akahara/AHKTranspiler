package fr.wonder.ahk.transpilers.common_x64.declarations;

public class EmptyLine implements Declaration {
	
	public static final EmptyLine INSTANCE = new EmptyLine();
	
	@Override
	public String toString() {
		return "";
	}
	
}
