package fr.wonder.ahk.compiled.units.sections;

public enum DeclarationVisibility {
	
	/** Global scope */
	GLOBAL,
	/** Unit scope */
	LOCAL,
	/** Section scope (ie: a function, an if body) */
	SECTION;
	
}
