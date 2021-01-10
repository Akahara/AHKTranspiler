package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiler.Unit;

public class LabeledStatement extends Statement {
	
	/** Set by the linker, see {@link SectionEndSt#closedStatement} */
	public SectionEndSt sectionEnd;
	public final boolean singleLine;
	
	public LabeledStatement(Unit unit, int sourceStart, int sourceStop, boolean singleLine, Expression... expressions) {
		super(unit, sourceStart, sourceStop, expressions);
		this.singleLine = singleLine;
	}
	
}
