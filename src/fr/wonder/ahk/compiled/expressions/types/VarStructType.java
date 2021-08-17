package fr.wonder.ahk.compiled.expressions.types;

import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;

public class VarStructType extends VarType {
	
	// FIX the equality check between types CANNOT be made using ==
	
	public final String name;
	
	/** Set by the Prelinker */
	public StructPrototype structure;
	
	public VarStructType(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String getSignature() {
		return String.format("S%02d", Math.floorMod(name.hashCode(), 100)); // FUTURE rework the struct signature
	}
	
	/**
	 * A struct does not have sub-types, it has members that may have different
	 * types but it itself is the targeted sub-type of other types.
	 */
	@Override
	public VarType[] getSubTypes() {
		return new VarType[0];
	}
	
	/**
	 * Two structure types are equal if the structure they point to are the same,
	 * as a single unit cannot import multiple structure with the same name this
	 * equality check can be done only by comparing the structures' names.
	 */
	@Override
	public boolean equals(Object other) {
		return other instanceof VarStructType &&
				((VarStructType) other).name.equals(name);
	}
	
	/** Needed for hashMaps, <b>cannot</b> be used before the linkage of the unit */
	@Override
	public int hashCode() {
		return structure.signature.hashCode();
	}
	
}