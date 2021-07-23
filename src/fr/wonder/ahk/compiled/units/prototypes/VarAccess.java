package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;

/**
 * A variable access if a reference made by {@link VarExp} to a symbolic
 * declaration that has a way of identifying itself by its signature.
 * 
 * <p>
 * They are collected per-unit in {@link UnitPrototype#externalAccesses} and are
 * the only references a unit is making to external declarations.
 * 
 * <p>
 * This is a good way to quickly know if a unit that did not change must be
 * recompiled: if one of these access did change the references must be re-validated,
 * otherwise even if one of the unit referenced did change but none of the signature
 * changed (a function was edited but not it signature for example), recompilation
 * need not happening.
 */
public interface VarAccess {

	/**
	 * Replaces {@link Signature#declaringUnit} if the variable is created inside a
	 * scope local to the unit.
	 */
	public static final String INNER_UNIT = "local";

	public Signature getSignature();

	public VarType getType();

	public boolean matchesDeclaration(ValueDeclaration decl);
	
	/** Returns true if the variable is declared inside a function scope */
	public default boolean isLocallyScoped() {
		return getSignature().declaringUnit == INNER_UNIT;
	}

}
