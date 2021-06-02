package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.UnitSource;

public class SectionEndSt extends Statement {
	
	/** Set by the linker, see {@link LabeledStatement#sectionEnd} */
	public LabeledStatement closedStatement;
	
	public SectionEndSt(UnitSource source, int sourceStart, int sourceStop) {
		super(source, sourceStart, sourceStop);
	}
	
	/**
	 * Used by the statement parser only, to create sections end for single line statements,
	 * in this case the section end does not appear in the source file and therefore has no
	 * text.
	 */
	public SectionEndSt(UnitSource source, int sourcePos) {
		this(source, sourcePos, -1);
	}

	@Override
	public String toString() {
		return "}";
	}
	
}
