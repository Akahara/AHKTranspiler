package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;

public class VariablePrototype implements VarAccess, Prototype<VariablePrototype> {
	
	public final Signature signature;
	public final VarType type;
	public final DeclarationModifiers modifiers;
	
	public VariablePrototype(Signature signature, VarType type, DeclarationModifiers modifiers) {
		this.signature = signature;
		this.type = type;
		this.modifiers = modifiers;
	}
	
	@Override
	public String toString() {
		return signature.declaringUnitName + "." + signature.name + ":" + type.toString();
	}

	@Override
	public VarType getType() {
		return type;
	}

	@Override
	public Signature getSignature() {
		return signature;
	}
	
}