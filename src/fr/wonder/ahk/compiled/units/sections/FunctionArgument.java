package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiler.linker.Signatures;

public class FunctionArgument implements VarAccess, SourceElement {
	
	public final SourceReference sourceRef;
	
	public final String name;
	public final VarType type;
	public final Signature signature;
	
	public FunctionArgument(SourceReference sourceRef, String name, VarType type) {
		this.sourceRef = sourceRef;
		this.name = name;
		this.type = type;
		this.signature = Signatures.of(this);
	}
	
	@Override
	public String toString() {
		return type + " " + name;
	}
	
	@Override
	public SourceReference getSourceReference() {
		return sourceRef;
	}
	
	@Override
	public VarType getType() {
		return type;
	}

	public Signature getSignature() {
		return signature;
	}
	
}
