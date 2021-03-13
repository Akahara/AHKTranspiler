package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiler.Unit;

public class VariablePrototype implements VarAccess, Prototype<VariableDeclaration> {
	
	public final Signature signature; // FIX ? make signatures private
	public final VarType type;
	
	public VariablePrototype(Signature signature, VarType type) {
		this.signature = signature;
		this.type = type;
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
		return signature.declaringUnitName + "." + signature.name + ":" + type.toString();
	}

	@Override
	public VarType getType() {
		return type;
	}

	@Override
	public String getDeclaringUnit() {
		return signature.declaringUnit;
	}

	public String getName() {
		return signature.name;
	}

	@Override
	public Signature getSignature() {
		return signature;
	}

	@Override
	public VariableDeclaration getAccess(Unit unit) {
		if(unit.fullBase.equals(getDeclaringUnit()))
			throw new IllegalArgumentException("Variable " + this + " is not declared in unit " + unit);
		for(VariableDeclaration v : unit.variables) {
			if(v.name.equals(signature.name) && v.getType().equals(type))
				return v;
		}
		return null;
	}
	
}