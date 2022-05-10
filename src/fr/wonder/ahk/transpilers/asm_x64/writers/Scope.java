package fr.wonder.ahk.transpilers.asm_x64.writers;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.LambdaClosureArgument;
import fr.wonder.ahk.transpilers.asm_x64.units.FunctionArgumentsLayout;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.Address;
import fr.wonder.ahk.transpilers.common_x64.addresses.LabelAddress;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;

class Scope {
	
	private final UnitWriter writer;
	private final FunctionArgumentsLayout sectionArguments;
	private final int stackSpace;
	
	private final List<VariableDeclaration> variables = new ArrayList<>();
	private final List<Integer> scopeSizes = new ArrayList<>();
	private int currentScopeSize = 0;
	
	private int stackOffset = 0;
	
	Scope(UnitWriter writer, FunctionArgumentsLayout sectionArguments, int sectionStackSpace) {
		this.writer = writer;
		this.sectionArguments = sectionArguments;
		this.stackSpace = sectionStackSpace;
	}
	
	MemAddress declareVariable(VariableDeclaration var) {
		variables.add(var);
		currentScopeSize++;
		return new MemAddress(Register.RSP, stackSpace-variables.size()*MemSize.POINTER_SIZE);
	}
	
	Address getVarAddress(VarAccess var) {
		// search through the function arguments
		if(var instanceof FunctionArgument)
			return sectionArguments.getArgumentLocation((FunctionArgument) var);
		if(var instanceof LambdaClosureArgument)
			return sectionArguments.getClosureArgumentLocation((LambdaClosureArgument) var);
		
		// search through the stack frame
		for(int i = 0; i < variables.size(); i++) {
			if(var == variables.get(i).getPrototype())
				return new MemAddress(Register.RSP, stackSpace-(i+1)*MemSize.POINTER_SIZE+stackOffset);
		}
		// search through global variables
		Address baseAddress = new LabelAddress(writer.registries.getRegistry(var));
		
		if(var instanceof FunctionPrototype)
			return baseAddress;
		else
			return new MemAddress(baseAddress);
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
