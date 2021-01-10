package fr.wonder.ahk.compiled.units;

import fr.wonder.ahk.compiler.Unit;

public class SourceObject implements SourceElement {
	
	public Unit declaringUnit;
	public final int sourceStart, sourceStop;
	
	public SourceObject(Unit declaringUnit, int sourceStart, int sourceStop) {
		this.declaringUnit = declaringUnit;
		this.sourceStart = sourceStart;
		this.sourceStop = sourceStop;
	}
	
	@Override
	public Unit getUnit() {
		return declaringUnit;
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
