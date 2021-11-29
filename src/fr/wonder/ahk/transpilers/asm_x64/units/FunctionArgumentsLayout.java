package fr.wonder.ahk.transpilers.asm_x64.units;

import fr.wonder.ahk.compiled.expressions.types.VarGenericType;
import fr.wonder.ahk.compiled.units.prototypes.blueprints.BlueprintPrototype;
import fr.wonder.ahk.compiled.units.prototypes.blueprints.GenericImplementationParameter;
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
	private final GenericImplementationParameter[] gips;
	
	public FunctionArgumentsLayout(FunctionArgument[] arguments, GenericImplementationParameter[] gips) {
		this.arguments = arguments;
		this.gips = gips;
	}
	
	public MemAddress getGIPLocation(VarGenericType genericInstance, BlueprintPrototype blueprint) {
		for(int i = 0; i < gips.length; i++) {
			GenericImplementationParameter gip = gips[i];
			if(gip.genericType.equals(genericInstance) &&
					gip.typeRequirement.blueprint.matchesPrototype(blueprint)) {
				return new MemAddress(Register.RBP, INTRINSIC_PADDING + (arguments.length + i) * MemSize.POINTER_SIZE);
			}
		}
		throw new IllegalArgumentException("Blueprint " + genericInstance + ":" + blueprint + " does not exist");
	}

	public Address getArgumentLocation(FunctionArgument var) {
		for(int i = 0; i < arguments.length; i++) {
			if(var == arguments[i])
				return new MemAddress(Register.RBP, INTRINSIC_PADDING + i * MemSize.POINTER_SIZE);
		}
		throw new IllegalArgumentException("Argument " + var + " does not exist");
	}

	public int getArgsStackSpace() {
		return (arguments.length + gips.length) * MemSize.POINTER_SIZE;
	}
	
}
