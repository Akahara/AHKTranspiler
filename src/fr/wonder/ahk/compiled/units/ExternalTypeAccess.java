package fr.wonder.ahk.compiled.units;

import fr.wonder.ahk.compiler.tokens.Token;

public class ExternalTypeAccess<T> {
	
	public final T typeInstance;
	public final SourceElement firstOccurrence;
	public int occurrenceCount;
	
	ExternalTypeAccess(T typeInstance, Token firstOccurence) {
		this.typeInstance = typeInstance;
		this.firstOccurrence = firstOccurence;
		this.occurrenceCount = 1;
	}
	
}
