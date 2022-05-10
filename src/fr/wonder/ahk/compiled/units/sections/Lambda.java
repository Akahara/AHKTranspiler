package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.utils.ArrayOperator;

public abstract class Lambda {

	public final SourceReference sourceRef;
	
	public final FunctionArgument[] args;
	public final LambdaClosureArgument[] closureArguments;
	
	public final VarFunctionType lambdaFunctionType;
	
	public Lambda(SourceReference sourceRef, FunctionArgument[] args, LambdaClosureArgument[] closureArguments, VarType returnType) {
		this.sourceRef = sourceRef;
		this.args = args;
		this.closureArguments = closureArguments;
		this.lambdaFunctionType = new VarFunctionType(returnType, getLambdaArgsTypes());
	}
	
	public VarType getReturnType() {
		return lambdaFunctionType.returnType;
	}
	
	public VarType[] getLambdaArgsTypes() {
		return ArrayOperator.map(args, VarType[]::new, FunctionArgument::getType);
	}

	public boolean hasClosureArguments() {
		return closureArguments.length != 0;
	}
	
	@Override
	public String toString() {
		return "Lambda(" + getReturnType() + ": " + Utils.toString(args) + ")[" + Utils.toString(closureArguments) + "]";
	}
	
}