package fr.wonder.ahk.compiled.units;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiler.tokens.Token;

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

	public static SourceReference concat(SourceReference ref1, SourceReference ref2) {
		if(ref1.source != ref2.source)
			throw new IllegalArgumentException("Cannot concat references of different sources");
		if(ref1.stop > ref2.start)
			throw new IllegalArgumentException("Invalid references to concat");
		return new SourceReference(ref1.source, ref1.start, ref2.stop);
	}
	
	public static SourceReference concat(SourceElement el1, SourceElement el2) {
		return concat(el1.getSourceReference(), el2.getSourceReference());
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
