package fr.wonder.ahk.compiled.units;

import fr.wonder.ahk.compiler.Unit;

public interface SourceElement {
	
	public Unit getUnit();
	public int getSourceStart();
	public int getSourceStop();
	
	public default String getErr() {
		return getUnit().source.getErr(getSourceStart(), getSourceStop());
	}
	
}
