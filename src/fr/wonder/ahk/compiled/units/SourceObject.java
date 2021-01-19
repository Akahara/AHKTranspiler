package fr.wonder.ahk.compiled.units;

import fr.wonder.ahk.UnitSource;

public class SourceObject implements SourceElement {
	
	private UnitSource source;
	public final int sourceStart, sourceStop;
	
	public SourceObject(UnitSource source, int sourceStart, int sourceStop) {
		this.source = source;
		this.sourceStart = sourceStart;
		this.sourceStop = sourceStop;
	}
	
	@Override
	public UnitSource getSource() {
		return source;
	}

	@Override
	public int getSourceStart() {
		return sourceStart;
	}

	@Override
	public int getSourceStop() {
		return sourceStop;
	}
	
}
