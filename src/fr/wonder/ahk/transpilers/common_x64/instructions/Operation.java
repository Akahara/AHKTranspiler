package fr.wonder.ahk.transpilers.common_x64.instructions;

/**
 * Represents an operation with an opcode and operands.
 * An operation is an instruction.
 */
public class Operation implements Instruction {
	
	public final OpCode opcode;

	public final OperationParameter[] operands;
	
	public Operation(OpCode opcode, OperationParameter... operands) {
		this.opcode = opcode;
		this.operands = operands;
	}
	
	@Override
	public String toString() {
		if(operands.length == 0)
			return opcode.toString().toLowerCase();
		String params = " " + operands[0].toString();
		for(int i = 1; i < operands.length; i++)
			params += "," + operands[i].toString();
		return opcode.toString().toLowerCase() + params;
	}
	
}
