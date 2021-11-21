package fr.wonder.ahk.transpilers.asm_x64.writers;

import fr.wonder.commons.exceptions.ErrorWrapper;

public class LambdaWriter extends FunctionWriter {
	
	public LambdaWriter(UnitWriter writer) {
		super(writer, null);
	}

	public void writeLambda(ErrorWrapper errors) {
		instructions.createStackFrame();
		instructions.endStackFrame();
	}

}
