package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.units.prototypes.VarAccess;

interface Scope {
	
	Scope innerScope();
	Scope outerScope();
	
	VarAccess getVariable(String name);
	UnitScope getUnitScope();
	
	void registerVariable(VarAccess var);
	
}