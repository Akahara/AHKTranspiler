package fr.wonder.ahk.transpilers.asm_x64.writers.operations;

import static fr.wonder.ahk.compiled.expressions.types.VarType.BOOL;
import static fr.wonder.ahk.compiled.expressions.types.VarType.FLOAT;
import static fr.wonder.ahk.compiled.expressions.types.VarType.INT;
import static fr.wonder.ahk.compiled.expressions.types.VarType.STR;

import java.util.HashMap;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarNativeType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.transpilers.asm_x64.writers.RegistryManager;
import fr.wonder.ahk.transpilers.common_x64.GlobalLabels;
import fr.wonder.ahk.transpilers.common_x64.InstructionSet;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnimplementedException;
import fr.wonder.commons.exceptions.UnreachableException;
import fr.wonder.commons.utils.Assertions;

class Conversions {

	static interface ConversionWriter {
		
		void write(VarType from, VarType to, AsmOperationWriter asmWriter, ErrorWrapper errors);
		
	}
	
	static interface ConversionFunctionWriter {
		
		void write(InstructionSet instructions);
		
	}

	static final Map<NativeConversion, ConversionWriter> conversions = new HashMap<>();
	static final Map<NativeConversion, ConversionFunctionWriter> conversionFunctions = new HashMap<>();
	
	private static void putConversion(VarNativeType from, VarNativeType to, ConversionWriter opWriter, ConversionFunctionWriter funcWriter) {
		NativeConversion key = new NativeConversion(from, to);
		conversions.put(key, opWriter);
		if(funcWriter != null)
			conversionFunctions.put(key, funcWriter);
	}
	
	static {
		putConversion(INT,   FLOAT, Conversions::conv_intTOfloat, Conversions::func_intTOfloat);
		putConversion(FLOAT, INT,   Conversions::conv_floatTOint, Conversions::func_floatTOint);
		putConversion(INT,   BOOL,  Conversions::conv_intTObool,  Conversions::func_intTObool );
		putConversion(BOOL,  INT,   (from, to, writer, errors) -> {}, (is) -> is.ret(MemSize.POINTER_SIZE)); // NOOP
		
		putConversion(INT,   STR, Conversions::conv_anyTOstr, null);
		putConversion(FLOAT, STR, Conversions::conv_anyTOstr, null);
		putConversion(BOOL,  STR, Conversions::conv_anyTOstr, null);
	}
	
	/**
	 * Checks and returns whether a conversion writer exists between two types.
	 * <p>
	 * If no conversion writer exists, an error is logged, the caller do not need to
	 * report anything more.
	 * <p>
	 * This function only checks that a <i>valid conversion</i> (in the sense defined
	 * by the AHK standard) has a writer implementation. It won't check to see if
	 * {@code from} can be converted to {@code to}.
	 */
	public static boolean checkHasConversionWriter(VarType from, VarType to, SourceElement source, ErrorWrapper errors) {
		if(from instanceof VarFunctionType && to instanceof VarFunctionType) {
			// unimplemented writer: a function type conversion cannot be recursive,
			// it is impossible to cast a ():():int to an ():():float or a
			// (():int):void to a (():float):void
			VarFunctionType f1 = (VarFunctionType) from;
			VarFunctionType f2 = (VarFunctionType) to;
			if(f1.returnType instanceof VarFunctionType && !f1.returnType.equals(f2.returnType)) {
				errors.add("x64t: Cannot deeply convert function types, the return type of " +
						from + " does not match the one of " + to + source.getErr());
				return false;
			}
			Assertions.assertTrue(f1.arguments.length == f2.arguments.length, "Invalid function type conversion: " + from + " to " + to);
			for(int i = 0; i < f1.arguments.length; i++) {
				if(f1.arguments[i] instanceof VarFunctionType && !f1.arguments[i].equals(f2.arguments[i])) {
					errors.add("x64t: Cannot deelpy convert function type, argument type " + f1.arguments[i]
							+ " of " + from + " does not match " + f2.arguments[i] + " of " + to + source.getErr());
					return false;
				}
			}
		}
		return true;
	}
	
	public static ConversionWriter getConversionWriter(VarType from, VarType to) {
		if(from instanceof VarNativeType && to instanceof VarNativeType) {
			// native to native
			NativeConversion key = new NativeConversion(from, to);
			ConversionWriter cw = conversions.get(key);
			if(cw == null)
				throw new UnreachableException("Unimplemented conversion " + from + " -> " + to);
			return cw;
		} else if(from instanceof VarFunctionType && to instanceof VarFunctionType) {
			// function to function
			return getFunctionConversionWriter((VarFunctionType) from, (VarFunctionType) to);
		}
		throw new UnreachableException("Unimplemented conversion: " + from + " -> " + to);
	}
	
	private static ConversionWriter getFunctionConversionWriter(VarFunctionType from, VarFunctionType to) {
		Assertions.assertTrue(from.arguments.length == to.arguments.length);
		
		String returnValueFunctionLabel = getConversionFunctionLabel(from.returnType, to.returnType);
		String[] conversionFunctionLabels = new String[from.arguments.length];
		for(int i = 0; i < from.arguments.length; i++)
			conversionFunctionLabels[i] = getConversionFunctionLabel(to.arguments[i], from.arguments[i]);

		return (_from, _to, asmWriter, errors) -> {
			asmWriter.writer.instructions.push(Register.RAX);
			asmWriter.writer.unitWriter.callAlloc(MemSize.POINTER_SIZE * (from.arguments.length + 4));
			// the new closure pointer is in $rax
			MemAddress closurePointerAddress = new MemAddress(Register.RAX);
			asmWriter.writer.instructions.pop(Register.RBX);
			String convClosureLabel = asmWriter.writer.unitWriter.requireExternLabel(GlobalLabels.CLOSURE_RUN_CASTED);
			asmWriter.writer.instructions.mov(closurePointerAddress.addOffset(MemSize.POINTER_SIZE*0), convClosureLabel);
			asmWriter.writer.instructions.mov(closurePointerAddress.addOffset(MemSize.POINTER_SIZE*1), from.arguments.length * MemSize.POINTER_SIZE);
			asmWriter.writer.instructions.mov(closurePointerAddress.addOffset(MemSize.POINTER_SIZE*2), Register.RBX);
			asmWriter.writer.unitWriter.requireExternLabel(returnValueFunctionLabel);
			asmWriter.writer.instructions.mov(closurePointerAddress.addOffset(MemSize.POINTER_SIZE*3), returnValueFunctionLabel);
			for(int i = 0; i < conversionFunctionLabels.length; i++) {
				asmWriter.writer.unitWriter.requireExternLabel(conversionFunctionLabels[i]);
				asmWriter.writer.instructions.mov(closurePointerAddress.addOffset(MemSize.POINTER_SIZE*(4+i)), conversionFunctionLabels[i]);
			}
		};
	}
	
	private static String getConversionFunctionLabel(VarType from, VarType to) {
		if(from.equals(to))
			return GlobalLabels.FUNCTION_NOOP;
		NativeConversion key = new NativeConversion(from, to);
		if(!conversionFunctions.containsKey(key))
			throw new UnreachableException("No conversion function from " + from + " to " + to);
		return RegistryManager.getConversionsClosureRegistry(key);
	}
	
	static void conv_intTOfloat(VarType from, VarType to, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		MemAddress floatst = asmWriter.writer.unitWriter.requireExternLabel(GlobalLabels.ADDRESS_FLOATST);
		asmWriter.writer.instructions.mov(floatst, Register.RAX);
		asmWriter.writer.instructions.addCasted(OpCode.FILD, MemSize.QWORD, floatst);
		asmWriter.writer.instructions.addCasted(OpCode.FSTP, MemSize.QWORD, floatst);
		asmWriter.writer.instructions.mov(Register.RAX, floatst);
	}
	
	static void func_intTOfloat(InstructionSet is) {
		MemAddress floatst = GlobalLabels.ADDRESS_FLOATST;
		is.mov(floatst, Register.RAX);
		is.addCasted(OpCode.FILD, MemSize.QWORD, floatst);
		is.addCasted(OpCode.FSTP, MemSize.QWORD, floatst);
		is.mov(Register.RAX, floatst);
		is.ret(MemSize.POINTER_SIZE);
	}

	static void conv_floatTOint(VarType from, VarType to, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		MemAddress floatst = asmWriter.writer.unitWriter.requireExternLabel(GlobalLabels.ADDRESS_FLOATST);
		asmWriter.writer.instructions.mov(floatst, Register.RAX);
		asmWriter.writer.instructions.addCasted(OpCode.FLD, MemSize.QWORD, floatst);
		asmWriter.writer.instructions.addCasted(OpCode.FISTTP, MemSize.QWORD, floatst);
		asmWriter.writer.instructions.mov(Register.RAX, floatst);
	}
	
	static void func_floatTOint(InstructionSet is) {
		MemAddress floatst = GlobalLabels.ADDRESS_FLOATST;
		is.mov(floatst, Register.RAX);
		is.addCasted(OpCode.FLD, MemSize.QWORD, floatst);
		is.addCasted(OpCode.FISTTP, MemSize.QWORD, floatst);
		is.mov(Register.RAX, floatst);
		is.ret(MemSize.POINTER_SIZE);
	}
	
	static void conv_intTObool(VarType from, VarType to, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		asmWriter.writer.instructions.test(Register.RAX);
		asmWriter.writer.instructions.mov(Register.RAX, 0);
		asmWriter.writer.instructions.add(OpCode.SETNZ, Register.AL);
	}
	
	static void func_intTObool(InstructionSet is) {
		is.test(Register.RAX);
		is.mov(Register.RAX, 0);
		is.add(OpCode.SETNZ, Register.AL);
		is.ret(MemSize.POINTER_SIZE);
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
