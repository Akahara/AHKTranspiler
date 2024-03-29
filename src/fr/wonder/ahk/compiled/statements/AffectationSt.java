package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.units.SourceReference;

public class AffectationSt extends Statement {
	
	public AffectationSt(SourceReference sourceRef,
			Expression variable, Expression value) {
		super(sourceRef, variable, value);
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
