package fr.wonder.ahk.transpilers.common_x64.declarations;

public class GlobalDeclaration implements Declaration {

	public final String label;
	
	public GlobalDeclaration(String label) {
		this.label = label;
	}
	
	@Override
	public String toString() {
		return "global " + label;
	}
}
