package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceObject;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;

public class FunctionArgument extends SourceObject implements ValueDeclaration, VarAccess {
	
	public final String name;
	public final VarType type;
	
	public FunctionArgument(int sourceStart, int sourceStop, String name, VarType type) {
		super(null, sourceStart, sourceStop);
		this.name = name;
		this.type = type;
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
	public String getDeclaringUnit() {
		return INNER_UNIT;
	}

	@Override
	public String getSignature() {
		return "arg_" + getName();
	}
	
}
