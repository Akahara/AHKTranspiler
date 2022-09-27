package fr.wonder.ahk.compiled.expressions.types;

import fr.wonder.ahk.compiled.units.prototypes.EnumPrototype;

public class VarEnumType extends VarUserDefinedType {

	public VarEnumType(String enumName) {
		super(enumName);
	}

	@Override
	public String getSignature() {
		return "enum_" + getName();
	}

	@Override
	public VarType[] getSubTypes() {
		return NO_SUBTYPES;
	}
	
	@Override
	public EnumPrototype getBackingType() {
		return (EnumPrototype) super.getBackingType();
	}
	
}
