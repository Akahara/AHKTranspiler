package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiler.Unit;

public class WhileSt extends LabeledStatement {

	public WhileSt(Unit unit, int sourceStart, int sourceStop, Expression condition, boolean singleLine) {
		super(unit, sourceStart, sourceStop, singleLine, condition);
	}
	
	public Expression getCondition() {
		return expressions[0];
	}
	
	@Override
	public String toString() {
		return "while(" + getCondition() + ")" + (singleLine ? "" : " {");
	}
	
}
