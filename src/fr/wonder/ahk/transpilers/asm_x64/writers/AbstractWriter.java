package fr.wonder.ahk.transpilers.asm_x64.writers;

import fr.wonder.ahk.transpilers.asm_x64.units.FunctionArgumentsLayout;
import fr.wonder.ahk.transpilers.asm_x64.writers.operations.AsmOperationWriter;
import fr.wonder.ahk.transpilers.common_x64.InstructionSet;

public class AbstractWriter {
	
	public final UnitWriter unitWriter;
	public final InstructionSet instructions;
	
	public final FunctionArgumentsLayout sectionArguments;
	public final MemoryManager mem;
	public final ExpressionWriter expWriter;
	public final AsmOperationWriter opWriter;
	public final AsmClosuresWriter closureWriter;
	
	public AbstractWriter(UnitWriter unitWriter, FunctionArgumentsLayout sectionArguments, int sectionStackSpace) {
		this.unitWriter = unitWriter;
		this.instructions = unitWriter.instructions;
		this.sectionArguments = sectionArguments;
		this.mem = new MemoryManager(this, sectionArguments, sectionStackSpace);
		this.expWriter = new ExpressionWriter(this);
		this.opWriter = new AsmOperationWriter(this);
		this.closureWriter = new AsmClosuresWriter(this);
	}

	
}
