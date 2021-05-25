package fr.wonder.ahk.transpilers.asm_x64.writers.memory;

import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.Address;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;

class FunctionScope extends SectionScope {
	
	private final FunctionSection func;
	
	FunctionScope(FunctionSection func, Scope unitScope) {
		super(unitScope);
		this.func = func;
	}
	
	@Override
	public Address getVarAddress(VarAccess var) {
		int loc = 16;
		for(FunctionArgument a : func.arguments) {
			if(var == a)
				return new MemAddress(Register.RBP, loc);
			else
				loc += MemSize.getPointerSize(a.getType()).bytes;
		}
		return super.getVarAddress(var);
	}
	
	@Override
	public int getTotalSize() {
		// prevent a call to parent#getTotalSize as unit scopes do not have total sizes
		return getSize();
	}
	
}
