package fr.wonder.ahk.transpilers.asm_x64.units;

import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.LambdaClosureArgument;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.Address;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;
import fr.wonder.commons.utils.Assertions;

public class FunctionArgumentsLayout {
	
	public static final LambdaClosureArgument[] NO_CLOSURE_ARGS = null;
	
	/**
	 * Padding that comes before the function arguments on the stack.
	 * This space contains the caller return address and the previous $rbp.
	 */
	private static final int INTRINSIC_PADDING = 2 * MemSize.QWORD.bytes;

	private final FunctionArgument[] arguments;
	private final LambdaClosureArgument[] lambdaClosureArguments;
	
	public FunctionArgumentsLayout(FunctionArgument[] arguments, LambdaClosureArgument[] lambdaClosureArguments) {
		this.arguments = arguments;
		this.lambdaClosureArguments = lambdaClosureArguments;
	}
	
	public Address getArgumentLocation(FunctionArgument var) {
		for(int i = 0; i < arguments.length; i++) {
			if(var == arguments[i])
				return new MemAddress(Register.RBP, INTRINSIC_PADDING + i * MemSize.POINTER_SIZE);
		}
		throw new IllegalArgumentException("Argument " + var + " does not exist");
	}
	
	public Address getClosureArgumentLocation(LambdaClosureArgument arg) {
		Assertions.assertTrue(hasClosureArguments(), "This function does not have closure arguments");
		MemAddress closureAddress = getClosureObjectAddress();
		for(int i = 0; i < lambdaClosureArguments.length; i++) {
			if(arg == lambdaClosureArguments[i])
				return new MemAddress(closureAddress, (1+i)*MemSize.POINTER_SIZE);
		}
		throw new IllegalArgumentException("Closure argument " + arg + " does not exist");
	}
	
	public MemAddress getClosureObjectAddress() {
		Assertions.assertTrue(hasClosureArguments(), "This function does not have closure arguments");
		return new MemAddress(Register.RBP, -MemSize.POINTER_SIZE);
	}
	
	public boolean hasClosureArguments() {
		return lambdaClosureArguments != NO_CLOSURE_ARGS;
	}

	public int getArgsStackSpace() {
		return arguments.length * MemSize.POINTER_SIZE;
	}
	
}
