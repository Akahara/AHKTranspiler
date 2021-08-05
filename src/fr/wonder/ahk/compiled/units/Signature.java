package fr.wonder.ahk.compiled.units;

import java.util.Objects;

public class Signature {
	
	/** Full base of the declaring unit */
	public final String declaringUnit;
	/** Name of the declaring unit */
	public final String declaringUnitName;
	
	/** In-Unit name, must not contain any unit-specific markers */
	public final String name;
	/** In-Unit signature, must not contain any unit-specific markers */
	public final String computedSignature;
	
	public Signature(String declaringUnit, String name, String computedSignature) {
		this.declaringUnit = Objects.requireNonNull(declaringUnit);
		this.declaringUnitName = declaringUnit.substring(declaringUnit.lastIndexOf('.')+1);
		this.name = Objects.requireNonNull(name);
		this.computedSignature = Objects.requireNonNull(computedSignature);
	}
	
	/**
	 * Returns true iff {@code other} is a Signature and both have the same
	 * declaring units and names
	 */
	@Override
	public boolean equals(Object other) {
		return other instanceof Signature &&
				((Signature) other).declaringUnit == declaringUnit &&
				((Signature) other).name.equals(name);
	}
	
	@Override
	public String toString() {
		return declaringUnit + ":" + computedSignature;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(declaringUnit, name, computedSignature);
	}
	
}
