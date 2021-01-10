package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.FunctionExpression;
import fr.wonder.ahk.compiler.Unit;

public class FunctionSt extends Statement {
	
	public FunctionSt(Unit unit, int sourceStart, int sourceStop, FunctionExpression function) {
		super(unit, sourceStart, sourceStop, function);
	}
	
	public FunctionExpression getFunction() {
		return (FunctionExpression) expressions[0];
	}
	
	@Override
	public String toString() {
		return getFunction().toString();
	}
	
}
