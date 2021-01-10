package fr.wonder.ahk.compiler.linker;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.ValueDeclaration;

class InnerScope implements Scope {
	
	final Scope parent;
	final List<ValueDeclaration> variables = new ArrayList<>();
	
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
	public ValueDeclaration getVariable(String name) {
		for(ValueDeclaration var : variables)
			if(var.getName().equals(name))
				return var;
		return parent.getVariable(name);
	}
	
	@Override
	public void registerVariable(ValueDeclaration var) {
		variables.add(var);
	}
}