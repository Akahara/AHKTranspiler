package fr.wonder.ahk.transpilers.asm_x64.units;

import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.Address;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;

public class FunctionArgumentsLayout {
	
	/**
	 * Padding that comes before the function arguments on the stack.
	 * This space contains the caller return address and the previous $rbp.
	 */
	private static final int INTRINSIC_PADDING = 2 * MemSize.QWORD.bytes;

	private final FunctionArgument[] arguments;
	
	public FunctionArgumentsLayout(FunctionArgument[] arguments) {
		this.arguments = arguments;
	}
	
	public Address getArgumentLocation(FunctionArgument var) {
		for(int i = 0; i < arguments.length; i++) {
			if(var == arguments[i])
				return new MemAddress(Register.RBP, INTRINSIC_PADDING + i * MemSize.POINTER_SIZE);
		}
		throw new IllegalArgumentException("Argument " + var + " does not exist");
	}

	public int getArgsStackSpace() {
		return arguments.length * MemSize.POINTER_SIZE;
	}
	
}
