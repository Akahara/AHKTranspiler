package fr.wonder.ahk.compiled.units.prototypes;

import java.util.Objects;

import fr.wonder.ahk.compiled.expressions.types.VarType;

public class VariablePrototype implements VarAccess {
	
	public final String declaringUnit;
	public final String name;
	public final String signature;
	public final VarType type;
	
	public VariablePrototype(String declaringUnit, String name, String signature, VarType type) {
		this.declaringUnit = Objects.requireNonNull(declaringUnit);
		this.name = Objects.requireNonNull(name);
		this.signature = Objects.requireNonNull(signature);
		this.type = Objects.requireNonNull(type);
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof VariablePrototype))
			return false;
		VariablePrototype p = (VariablePrototype) o;
		if(!signature.equals(p.signature))
			return false;
		if(!type.equals(p.type))
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return type.toString() + ":" + declaringUnit + "." + name;
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