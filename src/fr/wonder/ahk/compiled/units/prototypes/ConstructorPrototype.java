package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;

public class ConstructorPrototype implements Prototype<ConstructorPrototype> {

	public final Signature signature;
	public final VarType[] argTypes;
	public final String[] argNames;
	public final DeclarationModifiers modifiers;
	
	public ConstructorPrototype(VarType[] types, String[] names,
			DeclarationModifiers modifiers, Signature signature) {
		
		this.argTypes = types;
		this.argNames = names;
		this.modifiers = modifiers;
		this.signature = signature;
	}
	
	@Override
	public Signature getSignature() {
		return signature;
	}

}
