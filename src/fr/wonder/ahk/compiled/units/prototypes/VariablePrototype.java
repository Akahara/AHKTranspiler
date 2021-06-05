package fr.wonder.ahk.compiled.units.prototypes;

import java.util.Objects;

import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.commons.utils.Assertions;

public class VariablePrototype implements VarAccess, Prototype<VariableDeclaration> {
	
	public final Signature signature;
	public final VarType type;
	public final DeclarationModifiers modifiers;
	
	public VariablePrototype(Signature signature, VarType type, DeclarationModifiers modifiers) {
		this.signature = signature;
		this.type = type;
		this.modifiers = modifiers;
		Assertions.assertNonNull(signature, "Null signature");
		Assertions.assertNonNull(type, "Null type");
		Assertions.assertNonNull(modifiers, "Null modifiers");
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

	public String getName() {
		return signature.name;
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
	public VariableDeclaration getAccess(Unit unit) {
		if(unit.fullBase.equals(signature.declaringUnit))
			throw new IllegalArgumentException("Variable " + this + " is not declared in unit " + unit);
		for(VariableDeclaration v : unit.variables) {
			if(v.name.equals(signature.name) && v.getType().equals(type))
				return v;
		}
		return null;
	}

	@Override
	public boolean matchesDeclaration(ValueDeclaration decl) {
		return decl.getSignature().equals(getSignature());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(signature, type, modifiers);
	}
	
}