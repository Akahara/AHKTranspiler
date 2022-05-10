package fr.wonder.ahk.transpilers.asm_x64.writers;

import fr.wonder.ahk.compiled.units.sections.SimpleLambda;
import fr.wonder.ahk.transpilers.asm_x64.units.FunctionArgumentsLayout;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class LambdaWriter extends AbstractWriter {
	
	private final SimpleLambda lambda;
	
	public LambdaWriter(UnitWriter writer, SimpleLambda lambda) {
		super(writer, new FunctionArgumentsLayout(lambda.args, lambda.closureArguments), 0);
		this.lambda = lambda;
	}

	public void writeLambda(ErrorWrapper errors) {
		instructions.createStackFrame();
		
		// store closure arguments
		instructions.add(OpCode.SUB, Register.RSP, MemSize.POINTER_SIZE);
		instructions.mov(sectionArguments.getClosureObjectAddress(), Register.RAX); // the closure is stored at [rbp-8], which is [rsp] currently
		
		if(unitWriter.project.manifest.DEBUG_SYMBOLS)
			instructions.comment("~ " + lambda.getBody().toString());
		
		expWriter.writeExpression(lambda.getBody(), errors);
		instructions.endStackFrame();
		instructions.ret(sectionArguments.getArgsStackSpace());
	}

}
