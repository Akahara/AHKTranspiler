package fr.wonder.ahk.transpilers.common_x64.instructions;

import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.addresses.Address;

public class MovOperation extends Operation {

	public final MemSize cast;
	
	public MovOperation(Address to, OperationParameter from, MemSize cast) {
		super(OpCode.MOV, to, from);
		this.cast = cast;
	}
	
	public MovOperation(Address to, OperationParameter from) {
		this(to, from, null);
	}
	
	@Override
	public String toString() {
		return "mov " + (cast == null ? "":cast.name+" ") + operands[0] + "," + operands[1];
	}

}
