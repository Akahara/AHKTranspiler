package fr.wonder.ahk.transpilers.asm_x64.writers.operations;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.transpilers.common_x64.GlobalLabels;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnimplementedException;

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
		asmWriter.writer.instructions.addCasted(OpCode.FISTTP, MemSize.QWORD, floatst);
		asmWriter.writer.instructions.mov(Register.RAX, floatst);
	}
	
	static void conv_intTObool(VarType from, VarType to, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		asmWriter.writer.instructions.test(Register.RAX);
		asmWriter.writer.instructions.mov(Register.RAX, 0);
		asmWriter.writer.instructions.add(OpCode.SETNZ, Register.AL);
	}
	
	static void conv_anyTOstr(VarType from, VarType to, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		asmWriter.writer.instructions.push(Register.RAX);
		String conversionLabel;
		if(from == VarType.INT) {
			conversionLabel = GlobalLabels.AHK_STRINGS_INT2STR;
		} else if(from == VarType.FLOAT) {
			conversionLabel = GlobalLabels.AHK_STRINGS_FLOAT2STR;
		} else if(from == VarType.BOOL) {
			conversionLabel = GlobalLabels.AHK_STRINGS_BOOL2STR;
		} else {
			throw new UnimplementedException("Unimplemented convertion: " + from + " -> string");
		}
		asmWriter.writer.instructions.call(asmWriter.writer.unitWriter.requireExternLabel(conversionLabel));
	}

}
