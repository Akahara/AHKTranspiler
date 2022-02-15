package fr.wonder.ahk.compiled.expressions.types;

import fr.wonder.ahk.compiled.units.sections.TypeParameter;

public class VarGenericType extends VarType {
	
	public final TypeParameter typeParameter;
	
	public VarGenericType(TypeParameter typeParameter) {
		this.typeParameter = typeParameter;
	}
	
	@Override
	public String getName() {
		return String.valueOf(typeParameter.name);
	}
	
	@Override
	public String getSignature() {
		return "G" + typeParameter.name;
	}

	@Override
	public VarType[] getSubTypes() {
		return NO_SUBTYPES;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof VarGenericType && ((VarGenericType) o).typeParameter == typeParameter;
	}
	
	@Override
	public boolean hasGenericTyping() {
		return true;
	}
	
}
