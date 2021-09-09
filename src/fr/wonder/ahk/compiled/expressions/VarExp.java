package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;

public class VarExp extends Expression {
	
	public final String variable;
	
	// set by the linker
	public VarAccess declaration;
	
	public VarExp(SourceReference sourceRef, String variable) {
		super(sourceRef);
		this.variable = variable;
	}
	
	@Override
	public String toString() {
		return variable;
	}

}
