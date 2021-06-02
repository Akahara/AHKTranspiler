package fr.wonder.ahk.transpilers.asm_x64.writers;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.prototypes.Prototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.Address;
import fr.wonder.ahk.transpilers.common_x64.addresses.LabelAddress;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;

class Scope {
	
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
		return getVarAddress(var.getPrototype()); //TODO0 retrieve the created variable address more efficiently
	}
	
	Address getVarAddress(VarAccess var) {
		// search through the stack frame
		int loc = 0;
		for(VariableDeclaration v : variables) {
			if(var.matchesDeclaration(v))
				return new MemAddress(Register.RSP, stackSpace-loc+stackOffset);
			loc += MemSize.getPointerSize(v.getType()).bytes;
		}
		// search through the function arguments
		loc = 16;
		for(FunctionArgument a : func.arguments) {
			if(var.matchesDeclaration(a))
				return new MemAddress(Register.RBP, loc);
			loc += MemSize.getPointerSize(a.getType()).bytes;
		}
		// TODO check if #var is accessible
		// search through global variables
		return new MemAddress(new LabelAddress(
				var.getSignature().declaringUnit.equals(unit.fullBase) ?
					UnitWriter.getLocalRegistry((Prototype<?>) var) :
					UnitWriter.getGlobalRegistry((Prototype<?>) var)));
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
	
	/** Used when calling a function, to create space for the parameters */
	void setStackOffset(int offset) {
		if(stackOffset != 0 && offset != 0)
			throw new IllegalStateException("This scope already has a stack offset");
		stackOffset = offset;
	}
	
}
