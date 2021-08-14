package fr.wonder.ahk.compiled.units;

import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiler.tokens.Token;

public class ExternalStructAccess {
	
	public final VarStructType structTypeInstance;
	public final Token firstOccurence;
	public int occurenceCount;
	
	ExternalStructAccess(VarStructType structTypeInstance, Token firstOccurence) {
		this.structTypeInstance = structTypeInstance;
		this.firstOccurence = firstOccurence;
		this.occurenceCount = 1;
	}
	
}
