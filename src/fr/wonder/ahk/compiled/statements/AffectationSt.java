package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;

public class AffectationSt extends Statement {
	
	public AffectationSt(UnitSource source, int sourceStart, int sourceStop,
			Expression variable, Expression value) {
		super(source, sourceStart, sourceStop, variable, value);
	}
	
	public Expression getVariable() {
		return expressions[0];
	}
	
	public Expression getValue() {
		return expressions[1];
	}
	
	@Override
	public String toString() {
		return getVariable() + " = " + getValue();
	}

}
