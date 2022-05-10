package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiler.linker.ExpressionHolder;

public class SimpleLambda extends Lambda implements ExpressionHolder {
	
	private final Expression[] body; // is an array to match the ExpressionHolder interface, use #getBody
	
	public SimpleLambda(SourceReference sourceRef, VarType returnType, FunctionArgument[] args,
			LambdaClosureArgument[] closureArguments, Expression body) {
		super(sourceRef, args, closureArguments, returnType);
		this.body = new Expression[] { body };
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
	
}