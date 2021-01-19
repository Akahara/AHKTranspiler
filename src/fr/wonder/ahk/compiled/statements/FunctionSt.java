package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.FunctionExpression;

public class FunctionSt extends Statement {
	
	public FunctionSt(UnitSource source, int sourceStart, int sourceStop, FunctionExpression function) {
		super(source, sourceStart, sourceStop, function);
	}
	
	public FunctionExpression getFunction() {
		return (FunctionExpression) expressions[0];
	}
	
	@Override
	public String toString() {
		return getFunction().toString();
	}
	
}
