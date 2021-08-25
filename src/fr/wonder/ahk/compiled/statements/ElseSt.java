package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.units.SourceReference;

public class ElseSt extends LabeledStatement {
	
	/* Set by the linker */
	public IfSt closedIf;
	
	public ElseSt(SourceReference sourceRef, Expression condition, boolean singleLine) {
		super(sourceRef, singleLine, condition);
	}
	
	public ElseSt(SourceReference sourceRef, boolean singleLine) {
		super(sourceRef, singleLine);
	}
	
	public Expression getCondition() {
		return expressions.length == 0 ? null : expressions[0];
	}
	
	@Override
	public String toString() {
		return "else" + (getCondition()!=null ? "("+getCondition()+")" : "") + (singleLine?"":"{");
	}
	
}
