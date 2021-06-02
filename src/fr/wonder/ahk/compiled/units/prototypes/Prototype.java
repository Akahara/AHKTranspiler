package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiler.Unit;

/**
 * Prototypes are symbolic descriptions of accessible variables, functions
 * and units. They are used to describe data without actually knowing its
 * value. Refer to the child class for a more complete description.
 */
public interface Prototype<T extends ValueDeclaration> {
	
	public DeclarationModifiers getModifiers();
	public Signature getSignature();
	/** Returns the concrete variable this symbolic link refers to in the given unit */
	public T getAccess(Unit unit);
	public default boolean matchesDeclaration(ValueDeclaration decl) {
		return decl.getSignature().equals(getSignature());
	}
	
}
