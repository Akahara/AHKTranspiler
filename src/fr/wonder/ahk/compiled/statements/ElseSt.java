package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;

public class ElseSt extends LabeledStatement {
	
	/* Set by the linker */
	public IfSt closedIf;
	
	public ElseSt(UnitSource source, int sourceStart, int sourceStop, Expression condition, boolean singleLine) {
		super(source, sourceStart, sourceStop, singleLine, condition);
	}
	
	public ElseSt(UnitSource source, int sourceStart, int sourceStop, boolean singleLine) {
		super(source, sourceStart, sourceStop, singleLine);
	}
	
	public Expression getCondition() {
		return expressions.length == 0 ? null : expressions[0];
	}
	
	@Override
	public String toString() {
		return "else" + (getCondition()!=null ? "("+getCondition()+")" : "") + (singleLine?"":"{");
	}
	
}
