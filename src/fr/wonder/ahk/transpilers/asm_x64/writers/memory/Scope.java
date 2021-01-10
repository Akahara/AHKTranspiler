package fr.wonder.ahk.transpilers.asm_x64.writers.memory;

import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;

interface Scope {
	
	Scope getParent();
	VarLocation declareVariable(VariableDeclaration var);
	VarLocation getVarLocation(ValueDeclaration var);
	void addStackOffset(int offset);
	int getSize();
	int getTotalSize();
	
}
