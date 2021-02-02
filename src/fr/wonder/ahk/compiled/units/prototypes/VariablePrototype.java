package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.types.VarType;

public class VariablePrototype implements VarAccess {
	
	public final String declaringUnit;
	public final String name;
	public final String signature;
	public final VarType type;
	
	public VariablePrototype(String declaringUnit, String name, String signature, VarType type) {
		this.declaringUnit = declaringUnit;
		this.name = name;
		this.signature = signature;
		this.type = type;
	}

	@Override
	public VarType getType() {
		return type;
	}

	@Override
	public String getUnitFullBase() {
		return declaringUnit;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getSignature() {
		return signature;
	}
	
}