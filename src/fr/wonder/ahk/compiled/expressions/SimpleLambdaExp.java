package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.sections.SimpleLambda;
import fr.wonder.ahk.utils.Utils;

public class SimpleLambdaExp extends Expression {

	public final SimpleLambda lambda;
	
	public SimpleLambdaExp(SourceReference sourceRef, SimpleLambda lambda) {
		super(sourceRef);
		this.lambda = lambda;
	}
	
	@Override
	public String toString() {
		String returnType = lambda.returnType == VarType.VOID ? "" : lambda.returnType.toString();
		return "(" + returnType + ": " + Utils.toString(lambda.args) + ") => " + lambda.getBody().toString();
	}

}
