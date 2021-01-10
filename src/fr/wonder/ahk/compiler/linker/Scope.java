package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.expressions.ValueDeclaration;

interface Scope {
	
	Scope innerScope();
	Scope outerScope();
	
	ValueDeclaration getVariable(String name);
	UnitScope getUnitScope();
	
	void registerVariable(ValueDeclaration var);
	
}