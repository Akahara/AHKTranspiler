package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.units.SourceReference;

public class WhileSt extends LabeledStatement {

	public WhileSt(SourceReference sourceRef, Expression condition, boolean singleLine) {
		super(sourceRef, singleLine, condition);
	}
	
	public Expression getCondition() {
		return expressions[0];
	}
	
	@Override
	public String toString() {
		return "while(" + getCondition() + ")" + (singleLine ? "" : " {");
	}
	
}
