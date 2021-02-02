package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.types.Operation;

public class OperatorFunction implements Operation {

	public final FunctionPrototype function;
	
	public OperatorFunction(FunctionPrototype function) {
		this.function = function;
	}
	
	@Override
	public VarType getResultType() {
		return function.functionType.returnType;
	}

	@Override
	public VarType[] getOperandsTypes() {
		return function.functionType.arguments;
	}
	
}
