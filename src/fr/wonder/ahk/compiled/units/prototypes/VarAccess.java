package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;

public interface VarAccess {

	/**
	 * Replaces {@link Signature#declaringUnit} if the variable is created inside a
	 * scope in the unit.
	 */
	public static final String INNER_UNIT = "local";

	public Signature getSignature();
	
	/** Returns true if the variable is declared inside a function scope */
	public default boolean isLocallyScoped() {
		return getSignature().declaringUnit == INNER_UNIT;
	}

	public VarType getType();

}
