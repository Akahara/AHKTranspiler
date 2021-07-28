package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.units.Signature;

/**
 * Prototypes are symbolic descriptions of accessible variables, functions
 * and units. They are used to describe data without actually knowing their
 * value.
 */
public interface Prototype<T extends Prototype<T>> {
	
	public Signature getSignature();
	
	public default String getName() {
		return getSignature().name;
	}
	
	public default boolean matchesPrototype(T other) {
		return other instanceof Prototype<?> && ((Prototype<?>) other).getSignature().equals(getSignature());
	}
	
//	/** Returns the concrete variable this symbolic link refers to in the given unit */
//	public T getAccess(Unit unit);
	
}
