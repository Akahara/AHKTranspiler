package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiler.Unit;

public class ElseSt extends LabeledStatement {
	
	/* Set by the linker */
	public IfSt closedIf;
	
	public ElseSt(Unit unit, int sourceStart, int sourceStop, Expression condition, boolean singleLine) {
		super(unit, sourceStart, sourceStop, singleLine, condition);
	}
	
	public ElseSt(Unit unit, int sourceStart, int sourceStop, boolean singleLine) {
		super(unit, sourceStart, sourceStop, singleLine);
	}
	
	public Expression getCondition() {
		return expressions.length == 0 ? null : expressions[0];
	}
	
	@Override
	public String toString() {
		return "else" + (getCondition()!=null ? "("+getCondition()+")" : "") + (singleLine?"":"{");
	}
	
}
