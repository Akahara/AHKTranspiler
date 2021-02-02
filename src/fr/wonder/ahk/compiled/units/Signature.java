package fr.wonder.ahk.compiled.units;

public class Signature {
	
	public final String declaringUnit;
	
	/** In-Unit name, must not contain any unit-specific markers */
	public final String name;
	/** In-Unit signature, must not contain any unit-specific markers */
	public final String computedSignature;
	
	public Signature(String declaringUnit, String name, String computedSignature) {
		this.declaringUnit = declaringUnit;
		this.name = name;
		this.computedSignature = computedSignature;
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
	
}
