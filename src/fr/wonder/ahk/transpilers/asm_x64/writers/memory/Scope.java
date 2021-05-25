package fr.wonder.ahk.transpilers.asm_x64.writers.memory;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.transpilers.common_x64.addresses.Address;

public interface Scope {
	
	Scope getParent();
	Address declareVariable(VariableDeclaration var);
	Address getVarAddress(VarAccess var);
	void addStackOffset(int offset);
	int getSize();
	int getTotalSize();
	
}
