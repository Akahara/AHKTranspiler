package fr.wonder.ahk.compiler.linker;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.commons.exceptions.ErrorWrapper;

class InnerScope implements Scope {
	
	final Scope parent;
	final List<VarAccess> variables = new ArrayList<>();
	
	InnerScope(Scope parent) {
		this.parent = parent;
	}
	
	@Override
	public Scope outerScope() {
		return parent;
	}

	@Override
	public Scope innerScope() {
		return new InnerScope(this);
	}

	@Override
	public UnitScope getUnitScope() {
		return parent.getUnitScope();
	}
	
	@Override
	public VarAccess getVariable(String name, SourceElement srcElem, ErrorWrapper errors) {
		for(VarAccess var : variables) {
			if(var.getSignature().name.equals(name))
				return var;
		}
		return parent.getVariable(name, srcElem, errors);
	}
	
	@Override
	public void registerVariable(VarAccess var, SourceElement srcElem, ErrorWrapper errors) {
		if(isDeclaredVariable(var.getSignature().name))
			errors.add("Variable " + var.getSignature().name + " already exists in this scope" + srcElem.getErr());
		else
			variables.add(var);
	}
	
	private boolean isDeclaredVariable(String name) {
		InnerScope checkScope = this;
		while(true) {
			for(VarAccess v : variables)
				if(v.getSignature().name.equals(name))
					return true;
			if(checkScope.parent instanceof InnerScope)
				checkScope = (InnerScope) checkScope.parent;
			else
				return false;
		}
	}
}