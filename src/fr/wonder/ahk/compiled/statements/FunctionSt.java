package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.FunctionExpression;
import fr.wonder.ahk.compiled.units.SourceReference;

public class FunctionSt extends Statement {
	
	public FunctionSt(SourceReference sourceRef, FunctionExpression function) {
		super(sourceRef, function);
	}
	
	public FunctionExpression getFunction() {
		return (FunctionExpression) expressions[0];
	}
	
	@Override
	public String toString() {
		return getFunction().toString();
	}
	
}
