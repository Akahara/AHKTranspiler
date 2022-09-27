package fr.wonder.ahk.compiled.units;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.commons.utils.ArrayOperator;
import fr.wonder.commons.utils.Assertions;

public final class SourceReference {
	
	public final UnitSource source;
	public final int start, stop;
	
	public SourceReference(UnitSource source, int start, int stop) {
		this.source = source;
		this.start = start;
		this.stop = stop;
	}
	
	public String getErr() {
		return source.getErr(this);
	}
	
	public String getLine() {
		return source.getLine(this);
	}

	public static SourceReference concat(SourceReference... refs) {
		Assertions.assertTrue(refs.length > 1, "Cannot concat less than 2 references");
		for(int i = 1; i < refs.length; i++) {
			Assertions.assertTrue(refs[0].source == refs[i].source, "Cannot concat references of different sources");
			Assertions.assertTrue(refs[i].start >= refs[i-1].stop, "Ill-ordered references");
		}
		return new SourceReference(refs[0].source, refs[0].start, refs[refs.length-1].stop);
	}
	
	public static SourceReference concat(SourceElement... elems) {
		return concat(ArrayOperator.map(elems, SourceReference[]::new, SourceElement::getSourceReference));
	}
	
	public SourceReference collapseToEnd() {
		return new SourceReference(source, stop, stop);
	}

	public static SourceReference fromLine(Token[] line) {
		return fromLine(line, 0, line.length-1);
	}

	public static SourceReference fromLine(Token[] line, int first, int last) {
		if(first == last)
			return line[first].sourceRef;
		return concat(line[first].sourceRef, line[last].sourceRef);
	}
	
}
