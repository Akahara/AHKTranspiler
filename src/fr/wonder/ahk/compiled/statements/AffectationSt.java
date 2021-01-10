package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiler.Unit;

public class AffectationSt extends Statement {
	
	public AffectationSt(Unit unit, int sourceStart, int sourceStop, Expression variable, Expression value) {
		super(unit, sourceStart, sourceStop, variable, value);
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
