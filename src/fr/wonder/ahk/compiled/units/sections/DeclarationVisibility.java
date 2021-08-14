package fr.wonder.ahk.compiled.units.sections;

public enum DeclarationVisibility {
	
	/** Global scope */
	GLOBAL,
	/** Unit scope */
	LOCAL;
	
	/** Will match anything other than higher=local & lower=global */
	public static boolean isHigherOrder(DeclarationVisibility higher, DeclarationVisibility lower) {
		return higher == GLOBAL || lower == LOCAL;
	}
	
}
