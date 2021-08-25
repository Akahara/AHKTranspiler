package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;

/**
 * There is currently a 'bug' when using aliases that is really hard to fix and
 * will never happen randomly: There must be 4 units A, B and C, D, A and D are
 * declaring two structure with the same name, B is importing A and declaring an
 * alias using A's structure and C is importing B and D but not A, then the meaning
 * of B's alias in B and C are different. This won't really matter as aliases are
 * not types per-say and C won't be able to affect a D structure to an A variable.
 */
public class Alias {
	
	public final SourceReference sourceRef;
	
	public final String text;
	public final VarType resolvedType;
	public final DeclarationVisibility visibility = DeclarationVisibility.GLOBAL;
	
	public Alias(SourceReference sourceRef, String text, VarType resolvedType) {
		this.sourceRef = sourceRef;
		this.text = text;
		this.resolvedType = resolvedType;
	}

}
