package fr.wonder.ahk.compiled.units;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.types.VarBoundStructType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.units.sections.GenericContext;
import fr.wonder.ahk.compiler.tokens.Token;

public class ExternalStructAccess {
	
	public final VarStructType structTypeInstance;
	public final SourceElement firstOccurrence;
	public int occurrenceCount;
	
	public final List<ParametrizedAccess> parametrizedInstances = new ArrayList<>();
	
	ExternalStructAccess(VarStructType structTypeInstance, Token firstOccurence) {
		this.structTypeInstance = structTypeInstance;
		this.firstOccurrence = firstOccurence;
		this.occurrenceCount = 1;
	}
	
	public static class ParametrizedAccess {
		
		public VarBoundStructType type;
		public SourceElement occurrence;
		public GenericContext genericContext;
		
		public ParametrizedAccess(VarBoundStructType type, SourceElement occurrence, GenericContext genericContext) {
			this.type = type;
			this.occurrence = occurrence;
			this.genericContext = genericContext;
		}
		
	}
	
}
