package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.commons.annotations.Nullable;

public class ReturnSt extends Statement {
	
	public ReturnSt(SourceReference sourceRef, Expression expression) {
		super(sourceRef, expression);
	}
	
	public ReturnSt(SourceReference sourceRef) {
		super(sourceRef);
	}

	@Nullable
	public Expression getExpression() {
		return expressions.length == 0 ? null : expressions[0];
	}
	
	@Override
	public String toString() {
		return "return " + getExpression();
	}
	
}
