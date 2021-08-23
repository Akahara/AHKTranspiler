package fr.wonder.ahk.transpilers.asm_x64.writers.operations;

import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.ImmediateValue;
import fr.wonder.ahk.transpilers.common_x64.addresses.LabelAddress;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.ahk.transpilers.common_x64.instructions.OperationParameter;
import fr.wonder.commons.exceptions.ErrorWrapper;

class Jumps {

	static void jump_intEQUint(OperationExp exp, String label, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter rv = asmWriter.prepareRAXRBX(exp.getLeftOperand(), exp.getRightOperand(), true, errors);
		if(rv instanceof ImmediateValue && ((ImmediateValue)rv).text.equals("0"))
			asmWriter.writer.instructions.test(Register.RAX);
		else
			asmWriter.writer.instructions.cmp(Register.RAX, rv);
		asmWriter.writer.instructions.add(OpCode.JNE, new LabelAddress(label));
	}

	static void jump_intNEQUint(OperationExp exp, String label, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter rv = asmWriter.prepareRAXRBX(exp.getLeftOperand(), exp.getRightOperand(), true, errors);
		if(rv instanceof ImmediateValue && ((ImmediateValue)rv).text.equals("0"))
			asmWriter.writer.instructions.test(Register.RAX);
		else
			asmWriter.writer.instructions.cmp(Register.RAX, rv);
		asmWriter.writer.instructions.add(OpCode.JE, new LabelAddress(label));
	}

	static void jump_intLTint(OperationExp exp, String label, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter rv = asmWriter.prepareRAXRBX(exp.getLeftOperand(), exp.getRightOperand(), false, errors);
		asmWriter.writer.instructions.cmp(Register.RAX, rv);
		asmWriter.writer.instructions.add(OpCode.JGE, new LabelAddress(label));
	}

}
