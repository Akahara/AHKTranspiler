package fr.wonder.ahk.transpilers.asm_x64.writers.operations;

import static fr.wonder.ahk.compiled.expressions.Operator.*;
import static fr.wonder.ahk.compiled.expressions.types.VarType.INT;

import java.util.HashMap;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiler.types.NativeOperation;
import fr.wonder.ahk.compiler.types.Operation;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.ImmediateValue;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.ahk.transpilers.common_x64.instructions.OperationParameter;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.utils.Assertions;

class Jumps {
	
	private static final Map<Operation, JumpWriter> conditionalJumps = new HashMap<>();
	
	static interface JumpWriter {
		
		/** Writes the opcodes necessary to jump to the given label if the condition is <b>not</b> met */
		void write(OperationExp condition, String label, AsmOperationWriter asmWriter, ErrorWrapper errors);
		
	}
	
	static {
		conditionalJumps.put(NativeOperation.getOperation(INT, INT, EQUALS,  false), Jumps::jump_intEQUint );
		conditionalJumps.put(NativeOperation.getOperation(INT, INT, NEQUALS, false), Jumps::jump_intNEQUint);
		conditionalJumps.put(NativeOperation.getOperation(INT, INT, LOWER,   false), Jumps::jump_intLTint  );
		conditionalJumps.put(NativeOperation.getOperation(INT, INT, LEQUALS, false), Jumps::jump_intLEint  );
		conditionalJumps.put(NativeOperation.getOperation(INT, INT, GREATER, false), Jumps::jump_intGTint  );
		conditionalJumps.put(NativeOperation.getOperation(INT, INT, GEQUALS, false), Jumps::jump_intGEint  );
		
		Assertions.assertNull(conditionalJumps.get(null), "An unimplemented native operation was given an asm jump implementation");
	}
	
	public static JumpWriter getJumpWriter(Operation operation) {
		return conditionalJumps.get(operation);
	}

	static void jump_intEQUint(OperationExp exp, String label, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter rv = asmWriter.prepareRAXRBX(exp.getLeftOperand(), exp.getRightOperand(), true, errors);
		if(rv instanceof ImmediateValue && ((ImmediateValue)rv).text.equals("0"))
			asmWriter.writer.instructions.test(Register.RAX);
		else
			asmWriter.writer.instructions.cmp(Register.RAX, rv);
		asmWriter.writer.instructions.add(OpCode.JNE, label);
	}

	static void jump_intNEQUint(OperationExp exp, String label, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter rv = asmWriter.prepareRAXRBX(exp.getLeftOperand(), exp.getRightOperand(), true, errors);
		if(rv instanceof ImmediateValue && ((ImmediateValue)rv).text.equals("0"))
			asmWriter.writer.instructions.test(Register.RAX);
		else
			asmWriter.writer.instructions.cmp(Register.RAX, rv);
		asmWriter.writer.instructions.add(OpCode.JE, label);
	}

	static void jump_intLTint(OperationExp exp, String label, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter rv = asmWriter.prepareRAXRBX(exp.getLeftOperand(), exp.getRightOperand(), false, errors);
		asmWriter.writer.instructions.cmp(Register.RAX, rv);
		asmWriter.writer.instructions.add(OpCode.JGE, label);
	}

	static void jump_intLEint(OperationExp exp, String label, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter rv = asmWriter.prepareRAXRBX(exp.getLeftOperand(), exp.getRightOperand(), false, errors);
		asmWriter.writer.instructions.cmp(Register.RAX, rv);
		asmWriter.writer.instructions.add(OpCode.JG, label);
	}

	static void jump_intGTint(OperationExp exp, String label, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter rv = asmWriter.prepareRAXRBX(exp.getLeftOperand(), exp.getRightOperand(), false, errors);
		asmWriter.writer.instructions.cmp(Register.RAX, rv);
		asmWriter.writer.instructions.add(OpCode.JLE, label);
	}

	static void jump_intGEint(OperationExp exp, String label, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter rv = asmWriter.prepareRAXRBX(exp.getLeftOperand(), exp.getRightOperand(), false, errors);
		asmWriter.writer.instructions.cmp(Register.RAX, rv);
		asmWriter.writer.instructions.add(OpCode.JL, label);
	}

}
