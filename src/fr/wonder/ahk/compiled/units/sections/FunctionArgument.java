package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceObject;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;

public class FunctionArgument extends SourceObject implements ValueDeclaration, VarAccess {
	
	public final String name;
	public final VarType type;
	public final Signature signature;
	
	public FunctionArgument(UnitSource source, int sourceStart, int sourceStop, String name, VarType type) {
		super(source, sourceStart, sourceStop);
		this.name = name;
		this.type = type;
		this.signature = new Signature(INNER_UNIT, getName(), "arg_" + getName());
	}
	
	@Override
	public String toString() {
		return type + " " + name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public VarType getType() {
		return type;
	}

	@Override
	public DeclarationVisibility getVisibility() {
		return DeclarationVisibility.SECTION;
	}
	
	@Override
	public DeclarationModifiers getModifiers() {
		throw new IllegalStateException("Function arguments do not have modifiers");
	}

	@Override
	public Signature getSignature() {
		return signature;
	}
	
}
