package fr.wonder.ahk.compiler.types;

import fr.wonder.ahk.compiled.expressions.types.VarType;

public interface Operation {
	
	public VarType getResultType();
	/** Returns the left operand type */
	public VarType getLOType();
	/** Returns the right operand type */
	public VarType getROType();
	
}
