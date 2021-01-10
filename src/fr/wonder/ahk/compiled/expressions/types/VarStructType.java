package fr.wonder.ahk.compiled.expressions.types;

import fr.wonder.ahk.compiler.types.TypesTable;

public class VarStructType extends VarType {
	
	public final String name;
	/** set by the linker (TODO not implemented) */
	public VarStructType superType;
	
	public VarStructType(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String getSignature() {
		return String.format("%02d", Math.floorMod(name.hashCode(), 100));
	}
	
	/**
	 * There can only be one struct type (VarStructType instance) per actual structure (in the source code),
	 * all references are kept by the {@link TypesTable}.
	 * Therefore to check equality between two struct types can be done with the '==' operator.
	 */
	@Override
	public boolean equals(Object o) {
		return o == this;
	}
	
}