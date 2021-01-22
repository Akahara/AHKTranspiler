package fr.wonder.ahk.compiler.types;

import fr.wonder.ahk.compiled.expressions.types.VarType;

public interface Operation {
	
	public VarType getResultType();
	public VarType[] getOperandsTypes();
	
}
