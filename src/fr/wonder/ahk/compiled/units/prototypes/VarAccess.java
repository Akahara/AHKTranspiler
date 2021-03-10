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

	public VarType getType();

}
