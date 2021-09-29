package fr.wonder.ahk.transpilers.asm_x64.writers.operations;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.transpilers.common_x64.GlobalLabels;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.commons.exceptions.ErrorWrapper;

class Conversions {

	static void conv_intTOfloat(VarType from, VarType to, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		MemAddress floatst = asmWriter.writer.unitWriter.requireExternLabel(GlobalLabels.ADDRESS_FLOATST);
		asmWriter.writer.instructions.mov(floatst, Register.RAX);
		asmWriter.writer.instructions.addCasted(OpCode.FILD, MemSize.QWORD, floatst);
		asmWriter.writer.instructions.addCasted(OpCode.FSTP, MemSize.QWORD, floatst);
		asmWriter.writer.instructions.mov(Register.RAX, floatst);
	}

	static void conv_floatTOint(VarType from, VarType to, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		MemAddress floatst = asmWriter.writer.unitWriter.requireExternLabel(GlobalLabels.ADDRESS_FLOATST);
		asmWriter.writer.instructions.mov(floatst, Register.RAX);
		asmWriter.writer.instructions.addCasted(OpCode.FLD, MemSize.QWORD, floatst);
		asmWriter.writer.instructions.addCasted(OpCode.FISTP, MemSize.QWORD, floatst);
		asmWriter.writer.instructions.mov(Register.RAX, floatst);
	}

}
