package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.units.SourceReference;

public class WhileSt extends LabeledStatement {

	public final boolean isDoWhile;
	
	public WhileSt(SourceReference sourceRef, Expression condition, boolean singleLine, boolean isDoWhile) {
		super(sourceRef, singleLine, condition);
		this.isDoWhile = isDoWhile;
	}
	
	public Expression getCondition() {
		return expressions[0];
	}
	
	@Override
	public String toString() {
		return "while(" + getCondition() + ")" + (singleLine ? "" : " {");
	}
	
}
