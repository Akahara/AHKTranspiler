package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.utils.Utils;

public class MultiLineLambda extends Lambda {
	
	public MultiLineLambda(SourceReference sourceRef, VarType returnType, FunctionArgument[] args) {
		super(sourceRef, args, returnType);
	}
	
	@Override
	public String toString() {
		return "Lambda(" + (returnType == VarType.VOID ? "" : returnType) + ": " + Utils.toString(args) + ")";
	}

	public boolean hasClosureArguments() {
		return closureArguments.length != 0;
	}
	
}