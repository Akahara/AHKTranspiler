package fr.wonder.ahk.transpilers.common_x64.declarations;

public class ExternDeclaration implements Declaration {

	public final String label;
	
	public ExternDeclaration(String label) {
		this.label = label;
	}
	
	@Override
	public String toString() {
		return "extern " + label;
	}
}
