package fr.wonder.ahk.transpilers.asm_x64.writers;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.Prototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.Address;
import fr.wonder.ahk.transpilers.common_x64.addresses.LabelAddress;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;

class Scope {
	
	@SuppressWarnings("unused")
	private final Unit unit;
	private final FunctionSection func;
	private final int stackSpace;
	
	private final List<VariableDeclaration> variables = new ArrayList<>();
	private final List<Integer> scopeSizes = new ArrayList<>();
	private int currentScopeSize = 0;
	
	private int stackOffset = 0;
	
	Scope(Unit unit, FunctionSection func, int stackSpace) {
		this.unit = unit;
		this.func = func;
		this.stackSpace = stackSpace;
	}
	
	Address declareVariable(VariableDeclaration var) {
		variables.add(var);
		currentScopeSize++;
		return new MemAddress(Register.RSP, stackSpace-variables.size()*MemSize.POINTER_SIZE);
	}
	
	Address getVarAddress(VarAccess var) {
		// search through the stack frame
		for(int i = 0; i < variables.size(); i++) {
			if(var.matchesDeclaration(variables.get(i)))
				return new MemAddress(Register.RSP, stackSpace-(i+1)*MemSize.POINTER_SIZE+stackOffset);
		}
		// search through the function arguments
		for(int i = 0; i < func.arguments.length; i++) {
			if(var.matchesDeclaration(func.arguments[i]))
				return new MemAddress(Register.RBP, (i+2)*MemSize.POINTER_SIZE);
		}
		// TODO check if #var is accessible
		// search through global variables
		return new MemAddress(new LabelAddress(UnitWriter.getRegistry((Prototype<?>) var)));
	}
	
	void beginScope() {
		scopeSizes.add(currentScopeSize);
		currentScopeSize = 0;
	}
	
	void endScope() {
		while(currentScopeSize-- > 0)
			variables.remove(variables.size()-1);
		currentScopeSize = scopeSizes.remove(scopeSizes.size()-1);
	}
	
	void addStackOffset(int offset) {
		stackOffset += offset;
		if(offset < 0 && stackOffset < 0)
			throw new IllegalStateException("Popped to many bytes off the stack");
	}
	
}
