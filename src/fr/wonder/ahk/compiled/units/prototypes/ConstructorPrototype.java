package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;

public class ConstructorPrototype implements Prototype<ConstructorPrototype>, CallablePrototype {

	private final Signature signature;
	public final VarType[] types;
	public final String[] names;
	
	public ConstructorPrototype(VarType[] types, String[] names, Signature signature) {
		this.types = types;
		this.names = names;
		this.signature = signature;
	}
	
	@Override
	public Signature getSignature() {
		return signature;
	}

	@Override
	public VarType[] getArgumentTypes() {
		return types;
	}

}
