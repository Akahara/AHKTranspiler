package fr.wonder.ahk.compiled.expressions.types;

import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;

public class VarStructType extends VarUserDefinedType {
	
	public VarStructType(String name) {
		super(name);
	}
	
	@Override
	public String getSignature() {
		return String.format("S%02d", Math.floorMod(getName().hashCode(), 100)); // FUTURE rework the struct signature
	}
	
	@Override
	public StructPrototype getBackingType() {
		return (StructPrototype) super.getBackingType();
	}
	
	/**
	 * A struct does not have sub-types, it has members that may have different
	 * types but it itself is the targeted sub-type of other types.
	 */
	@Override
	public VarType[] getSubTypes() {
		return NO_SUBTYPES;
	}
	
}