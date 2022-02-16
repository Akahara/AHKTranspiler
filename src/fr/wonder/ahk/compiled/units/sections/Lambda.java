package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.commons.utils.ArrayOperator;

public abstract class Lambda {

	public final SourceReference sourceRef;
	
	public final VarType returnType;
	public final FunctionArgument[] args;
	public final LambdaClosureArgument[] closureArguments = new LambdaClosureArgument[0]; // TODO implement lambda closure arguments
	
	public final VarFunctionType lambdaFunctionType;
	
	public Lambda(SourceReference sourceRef, FunctionArgument[] args, VarType returnType) {
		this.sourceRef = sourceRef;
		this.args = args;
		this.returnType = returnType;
		this.lambdaFunctionType = new VarFunctionType(returnType, getLambdaArgsTypes());
	}
	
	public VarType[] getLambdaArgsTypes() {
		return ArrayOperator.map(args, VarType[]::new, FunctionArgument::getType);
	}
	
}