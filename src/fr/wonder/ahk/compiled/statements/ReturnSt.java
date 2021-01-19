package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;

public class ReturnSt extends Statement {
	
	public ReturnSt(UnitSource source, int sourceStart, int sourceStop, Expression expression) {
		super(source, sourceStart, sourceStop, expression);
	}
	
	public ReturnSt(UnitSource source, int sourceStart, int sourceStop) {
		super(source, sourceStart, sourceStop);
	}

	public Expression getExpression() {
		return expressions.length == 0 ? null : expressions[0];
	}
	
	@Override
	public String toString() {
		return "return " + getExpression();
	}
	
}
