package fr.wonder.ahk.compiled.units.prototypes;

import java.util.Arrays;

import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.commons.utils.ArrayOperator;

public class EnumPrototype implements Prototype<EnumPrototype>, TypeAccess {

	public final String enumName;
	public final String[] enumValues;
	public final DeclarationModifiers modifiers;
	public final Signature signature;
	
	public EnumPrototype(String enumName, String[] enumValues, DeclarationModifiers modifiers, Signature signature) {
		this.enumName = enumName;
		this.enumValues = enumValues;
		this.modifiers = modifiers;
		this.signature = signature;
	}
	
	public boolean hasValue(String valueName) {
		return ArrayOperator.contains(enumValues, valueName);
	}
	
	@Override
	public Signature getSignature() {
		return signature;
	}

	@Override
	public DeclarationModifiers getModifiers() {
		return modifiers;
	}
	
	@Override
	public boolean matchesPrototype(EnumPrototype other) {
		return signature.equals(other.signature) && Arrays.equals(enumValues, other.enumValues);
	}

}
