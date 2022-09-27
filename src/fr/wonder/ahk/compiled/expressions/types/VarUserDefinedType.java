package fr.wonder.ahk.compiled.expressions.types;

import fr.wonder.ahk.compiled.units.prototypes.Prototype;

public abstract class VarUserDefinedType extends VarType {
	
	private final String name;
	private Prototype<?> backingType;
	
	public VarUserDefinedType(String name) {
		this.name = name;
	}
	
	/**
	 * Called by the Prelinker
	 */
	public void setBackingType(Prototype<?> backingType) {
		if(this.backingType != null)
			throw new IllegalStateException("Backing type already set");
		this.backingType = backingType;
	}
	
	public Prototype<?> getBackingType() {
//		return Objects.requireNonNull(backingType, "This type was not prelinked"); // not checked for optimization purpose
		return backingType;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	/**
	 * Two user defined types are equal if the concrete type they point to are the same,
	 * as a single unit cannot import multiple concrete types with the same name, this
	 * equality check can be done only by comparing the types' names.
	 */
	@Override
	public boolean equals(Object o) {
		return o.getClass().equals(getClass()) && ((VarUserDefinedType) o).name.equals(name);
	}
	
	/** Needed for hashMaps, <b>cannot</b> be used before the linkage of the unit */
	@Override
	public int hashCode() {
		return backingType.getSignature().hashCode();
	}
	
}
