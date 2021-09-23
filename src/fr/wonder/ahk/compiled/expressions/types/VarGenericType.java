package fr.wonder.ahk.compiled.expressions.types;

import fr.wonder.ahk.compiled.units.sections.BlueprintRef;

public class VarGenericType extends VarType {
	
	public static final BlueprintRef[] NO_TYPE_RESTRICTION = new BlueprintRef[0];
	
	public final String name;
	public final BlueprintRef[] typeRestrictions;
	
	public VarGenericType(String name, BlueprintRef[] typeRestrictions) {
		this.name = name;
		this.typeRestrictions = typeRestrictions;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getSignature() {
		return "G0";
	}

	@Override
	public VarType[] getSubTypes() {
		return NO_SUBTYPES;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof VarGenericType && ((VarGenericType) o).name.equals(name);
	}
	
	@Override
	public boolean hasGenericTyping() {
		return true;
	}
	
}
