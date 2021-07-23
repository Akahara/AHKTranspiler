package fr.wonder.ahk.compiled.units;

import fr.wonder.ahk.UnitSource;

/**
 * A source element is an object that has a concrete text
 * segment associated with it in the unit source, it can
 * be used to print errors and located in the unit source.
 */
public interface SourceElement {
	
	public UnitSource getSource();
	public int getSourceStart();
	public int getSourceStop();
	
	/**
	 * Returns a string starting with '\n' containing this element's
	 * source line, highlighted on the textual occurrence of this element.
	 */
	public default String getErr() {
		return getSource().getErr(getSourceStart(), getSourceStop());
	}
	
}
