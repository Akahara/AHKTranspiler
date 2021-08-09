package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;

/**
 * Labeled statements are statements such as if, else, for(each)... that open a
 * new scope.
 */
public abstract class LabeledStatement extends Statement {
	
	/** Set by the linker, see {@link SectionEndSt#closedStatement} */
	public SectionEndSt sectionEnd;
	public final boolean singleLine;
	
	public LabeledStatement(UnitSource source, int sourceStart, int sourceStop,
			boolean singleLine, Expression... expressions) {
		super(source, sourceStart, sourceStop, expressions);
		this.singleLine = singleLine;
	}
	
}
