package fr.wonder.ahk.transpilers.asm_x64.writers.memory;

import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;

class FunctionScope extends SectionScope {
	
	private final FunctionSection func;
	
	FunctionScope(FunctionSection func, Scope unitScope) {
		super(unitScope);
		this.func = func;
	}
	
	@Override
	public VarLocation getVarLocation(ValueDeclaration var) {
		int loc = 16;
		for(FunctionArgument a : func.arguments) {
			if(var == a)
				return new MemoryLoc(VarLocation.REG_RBP, loc);
			else
				loc += a.getType().getSize();
		}
		return super.getVarLocation(var);
	}
	
	@Override
	public int getTotalSize() {
		// prevent a call to parent#getTotalSize as unit scopes do not have total sizes
		return getSize();
	}
	
}
