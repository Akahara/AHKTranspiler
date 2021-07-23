package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.StructConstructor;
import fr.wonder.commons.exceptions.UnimplementedException;

public class ConstructorPrototype implements Prototype<StructConstructor>, CallablePrototype {

	private final Signature signature;
	public final VarType[] types;
	public final String[] names;
	
	public ConstructorPrototype(VarType[] types, String[] names, Signature signature) {
		this.types = types;
		this.names = names;
		this.signature = signature;
	}
	
	@Override
	public DeclarationModifiers getModifiers() {
		return DeclarationModifiers.NONE;
	}

	@Override
	public Signature getSignature() {
		return signature;
	}

	@Override
	public StructConstructor getAccess(Unit unit) {
		throw new UnimplementedException();
	}

	@Override
	public VarType[] getArgumentTypes() {
		return types;
	}

}
