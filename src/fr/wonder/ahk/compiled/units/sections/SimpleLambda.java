package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiler.linker.ExpressionHolder;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.utils.ArrayOperator;

public class SimpleLambda implements ExpressionHolder {
	
	private final SourceReference sourceRef;
	
	public final VarType returnType;
	public final FunctionArgument[] args;
	private final Expression[] body; // is an array to match the ExpressionHolder interface, use #getBody
	public final LambdaClosureArgument[] closureArguments = new LambdaClosureArgument[0]; // TODO implement lambda closure arguments
	
	public final VarFunctionType lambdaFunctionType;
	
	public SimpleLambda(SourceReference sourceRef, VarType returnType, FunctionArgument[] args, Expression body) {
		this.sourceRef = sourceRef;
		this.returnType = returnType;
		this.args = args;
		this.body = new Expression[] { body };
		this.lambdaFunctionType = new VarFunctionType(returnType, getLambdaArgsTypes(), GenericContext.NO_CONTEXT);
	}
	
	@Override
	public String toString() {
		return "Lambda(" + (returnType == VarType.VOID ? "" : returnType) + ": " + Utils.toString(args) + ")";
	}
	
	public Expression getBody() {
		return body[0];
	}

	@Override
	public SourceReference getSourceReference() {
		return sourceRef;
	}

	@Override
	public Expression[] getExpressions() {
		return body;
	}
	
	public VarType[] getLambdaArgsTypes() {
		return ArrayOperator.map(args, VarType[]::new, FunctionArgument::getType);
	}

	public boolean hasClosureArguments() {
		return closureArguments.length != 0;
	}
	
}
