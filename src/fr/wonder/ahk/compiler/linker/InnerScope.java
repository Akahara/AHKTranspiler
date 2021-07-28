package fr.wonder.ahk.compiler.linker;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.units.prototypes.VarAccess;

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
	public VarAccess getVariable(String name) {
		for(VarAccess var : variables) {
			if(var.getSignature().name.equals(name))
				return var;
		}
		return parent.getVariable(name);
	}
	
	@Override
	public void registerVariable(VarAccess var) {
		variables.add(var);
	}
}