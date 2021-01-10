package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiler.Unit;

public class ReturnSt extends Statement {
	
	public ReturnSt(Unit unit, int sourceStart, int sourceStop, Expression expression) {
		super(unit, sourceStart, sourceStop, expression);
	}
	
	public ReturnSt(Unit unit, int sourceStart, int sourceStop) {
		super(unit, sourceStart, sourceStop);
	}

	public Expression getExpression() {
		return expressions.length == 0 ? null : expressions[0];
	}
	
	@Override
	public String toString() {
		return "return " + getExpression();
	}
	
}
