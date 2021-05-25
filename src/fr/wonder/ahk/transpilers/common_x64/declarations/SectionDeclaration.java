package fr.wonder.ahk.transpilers.common_x64.declarations;

public class SectionDeclaration implements Declaration {

	public static final String DATA = ".data";
	public static final String TEXT = ".text";
	
	public final String section;
	
	public SectionDeclaration(String section) {
		this.section = section;
	}
	
	@Override
	public String toString() {
		return "section " + section;
	}
	
}
