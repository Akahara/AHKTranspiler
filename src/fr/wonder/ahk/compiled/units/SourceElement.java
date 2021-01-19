package fr.wonder.ahk.compiled.units;

import fr.wonder.ahk.UnitSource;

public interface SourceElement {
	
	public UnitSource getSource();
	public int getSourceStart();
	public int getSourceStop();
	
	public default String getErr() {
		return getSource().getErr(getSourceStart(), getSourceStop());
	}
	
}
