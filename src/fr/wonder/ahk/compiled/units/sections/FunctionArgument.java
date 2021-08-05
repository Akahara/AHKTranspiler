package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceObject;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiler.linker.Signatures;

public class FunctionArgument extends SourceObject implements VarAccess {
	
	public final String name;
	public final VarType type;
	public final Signature signature;
	
	public FunctionArgument(UnitSource source, int sourceStart,
			int sourceStop, String name, VarType type) {
		super(source, sourceStart, sourceStop);
		this.name = name;
		this.type = type;
		this.signature = Signatures.of(this);
	}
	
	@Override
	public String toString() {
		return type + " " + name;
	}

	@Override
	public VarType getType() {
		return type;
	}

	public Signature getSignature() {
		return signature;
	}
	
}
