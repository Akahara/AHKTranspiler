package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;

public class WhileSt extends LabeledStatement {

	public WhileSt(UnitSource source, int sourceStart, int sourceStop, Expression condition, boolean singleLine) {
		super(source, sourceStart, sourceStop, singleLine, condition);
	}
	
	public Expression getCondition() {
		return expressions[0];
	}
	
	@Override
	public String toString() {
		return "while(" + getCondition() + ")" + (singleLine ? "" : " {");
	}
	
}
