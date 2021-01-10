package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiler.Unit;

public class SectionEndSt extends Statement {
	
	/** Set by the linker, see {@link LabeledStatement#sectionEnd} */
	public LabeledStatement closedStatement;
	
	public SectionEndSt(Unit unit, int sourceStart, int sourceStop) {
		super(unit, sourceStart, sourceStop);
	}
	
	/**
	 * Used by the statement parser only, to create sections end for single line statements,
	 * in this case the section end does not appear in the source file and therefore has no
	 * text.
	 */
	public SectionEndSt(Unit unit, int sourcePos) {
		this(unit, sourcePos, sourcePos);
	}

	@Override
	public String toString() {
		return "}";
	}
	
}
