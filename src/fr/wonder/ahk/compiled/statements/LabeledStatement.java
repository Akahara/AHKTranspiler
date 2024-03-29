package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.units.SourceReference;

/**
 * Labeled statements are statements such as if, else, for(each)... that open a
 * new scope.
 */
public abstract class LabeledStatement extends Statement {
	
	/** Set by the linker, see {@link SectionEndSt#closedStatement} */
	public SectionEndSt sectionEnd;
	public final boolean singleLine;
	
	public LabeledStatement(SourceReference sourceRef,
			boolean singleLine, Expression... expressions) {
		super(sourceRef, expressions);
		this.singleLine = singleLine;
	}
	
}
