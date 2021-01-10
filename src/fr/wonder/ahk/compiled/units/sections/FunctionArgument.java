package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceObject;

public class FunctionArgument extends SourceObject implements ValueDeclaration {
	
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
	
}
