package fr.wonder.ahk.compiled.units;

public interface SourceElement {
	
	public SourceReference getSourceReference();
	
	public default String getErr() {
		return getSourceReference().getErr();
	}
	
}
