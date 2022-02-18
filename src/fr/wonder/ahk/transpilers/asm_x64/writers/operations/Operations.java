package fr.wonder.ahk.transpilers.asm_x64.writers.operations;

import static fr.wonder.ahk.compiled.expressions.Operator.*;
import static fr.wonder.ahk.compiled.expressions.types.VarType.BOOL;
import static fr.wonder.ahk.compiled.expressions.types.VarType.FLOAT;
import static fr.wonder.ahk.compiled.expressions.types.VarType.INT;
import static fr.wonder.ahk.compiled.expressions.types.VarType.STR;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarNativeType;
import fr.wonder.ahk.compiler.types.NativeOperation;
import fr.wonder.ahk.transpilers.common_x64.GlobalLabels;
import fr.wonder.ahk.transpilers.common_x64.InstructionSet;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.Address;
import fr.wonder.ahk.transpilers.common_x64.addresses.ImmediateValue;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.ahk.transpilers.common_x64.instructions.OperationParameter;
import fr.wonder.ahk.transpilers.common_x64.instructions.SpecialInstruction;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnimplementedException;

class Operations {

	static interface OperationWriter {
		
		void write(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors);
		
	}
	
	static interface NativeFunctionWriter {
		
		void write(InstructionSet instructions);
		
	}
	
	private static final int F_COMMUTATIVE = 1 << 0;
	private static final int F_SWAP_ARGS = 1 << 1;
	private static final int F_POPAFTER = 1 << 2;
	
	static final Map<NativeOperation, OperationWriter> nativeOperations = new HashMap<>();
	static final Map<NativeOperation, NativeFunctionWriter> nativeFunctions = new HashMap<>();
	
	private static void putOperation(VarNativeType l, VarNativeType r, Operator o, OperationWriter opWriter, NativeFunctionWriter funcWriter) {
		NativeOperation op = NativeOperation.getOperation(l, r, o, false);
		if(op == null)
			throw new IllegalStateException("An unimplemented native operation was given an asm implementation");
		nativeOperations.put(op, opWriter);
		if(funcWriter != null)
			nativeFunctions.put(op, funcWriter);
	}
	
	static {
		putOperation(INT, INT, ADD, Operations::op_intADDint, Operations::fc_intADDint);
		putOperation(INT, INT, SUBSTRACT, Operations::op_intSUBint, Operations::fc_intSUBint);
		putOperation(null, INT, SUBSTRACT, Operations::op_nullSUBint, Operations::fc_nullSUBint);
		putOperation(INT, INT, MULTIPLY, Operations::op_intMULint, Operations::fc_intMULint);
		putOperation(INT, INT, DIVIDE, Operations::op_intDIVint, Operations::fc_intDIVint);
		putOperation(INT, INT, MOD, Operations::op_intMODint, Operations::fc_intMODint);
		putOperation(INT, INT, SHR, Operations::op_intSHRint, null);
		putOperation(INT, INT, SHL, Operations::op_intSHLint, null);
		putOperation(INT, INT, POWER, Operations::op_intPOWERint, Operations::fc_intPOWERint);
		putOperation(INT, INT, LOWER, opSimpleCmpOperation(OpCode.SETL), fcSimpleCmpOperation(OpCode.SETL));
		putOperation(INT, INT, LEQUALS, opSimpleCmpOperation(OpCode.SETLE), fcSimpleCmpOperation(OpCode.SETLE));
		putOperation(INT, INT, GREATER, opSimpleCmpOperation(OpCode.SETG), fcSimpleCmpOperation(OpCode.SETG));
		putOperation(INT, INT, GEQUALS, opSimpleCmpOperation(OpCode.SETLE), fcSimpleCmpOperation(OpCode.SETGE));
		
		putOperation(null, FLOAT, SUBSTRACT, Operations::op_nullSUBfloat, Operations::fc_nullSUBfloat);
		putOperation(FLOAT, FLOAT, ADD, opSimpleFPUOperation(OpCode.FADDP, F_COMMUTATIVE), fcSimpleFPUOperation(OpCode.FADDP, 0));
		putOperation(FLOAT, FLOAT, MULTIPLY, opSimpleFPUOperation(OpCode.FMULP, F_COMMUTATIVE), fcSimpleFPUOperation(OpCode.FMULP, 0));
		putOperation(FLOAT, FLOAT, SUBSTRACT, opSimpleFPUOperation(OpCode.FSUBP, 0), fcSimpleFPUOperation(OpCode.FSUBP, 0));
		putOperation(FLOAT, FLOAT, DIVIDE, opSimpleFPUOperation(OpCode.FDIVP, 0), fcSimpleFPUOperation(OpCode.FDIVP, 0));
		putOperation(FLOAT, FLOAT, MOD, opSimpleFPUOperation(OpCode.FPREM, F_SWAP_ARGS | F_POPAFTER), fcSimpleFPUOperation(OpCode.FPREM, F_SWAP_ARGS | F_POPAFTER));
		putOperation(FLOAT, FLOAT, POWER, Operations::op_floatPOWERfloat, Operations::fc_floatPOWERfloat);
		
		putOperation(BOOL, BOOL, EQUALS, Operations::op_universalEquality, Operations::fc_universalEquality);
		putOperation(INT, INT, EQUALS, Operations::op_universalEquality, Operations::fc_universalEquality);
		putOperation(FLOAT, FLOAT, EQUALS, Operations::op_universalEquality, Operations::fc_universalEquality);
		putOperation(BOOL, BOOL, NEQUALS, Operations::op_universalNEquality, Operations::fc_universalNEquality);
		putOperation(INT, INT, NEQUALS, Operations::op_universalNEquality, Operations::fc_universalNEquality);
		putOperation(FLOAT, FLOAT, NEQUALS, Operations::op_universalNEquality, Operations::fc_universalNEquality);
		
		putOperation(null, BOOL, NOT, Operations::op_nullNOTbool, Operations::fc_nullNOTbool);
		putOperation(STR, STR, ADD, Operations::op_strADDstr, Operations::fc_strADDstr);

		putOperation(BOOL, BOOL, OR, Operations::op_boolORbool, Operations::fc_boolORbool);
		putOperation(INT, INT, OR, Operations::op_boolORbool, Operations::fc_boolORbool);
		putOperation(BOOL, BOOL, AND, Operations::op_boolANDbool, Operations::fc_boolANDbool);
		putOperation(INT, INT, AND, Operations::op_boolANDbool, Operations::fc_boolANDbool);

		putOperation(BOOL, BOOL, BITWISE_OR, Operations::op_intBITORint, Operations::fc_intBITORint);
		putOperation(BOOL, BOOL, BITWISE_AND, Operations::op_intBITANDint, Operations::fc_intBITANDint);
		putOperation(INT, INT, BITWISE_OR, Operations::op_intBITORint, Operations::fc_intBITORint);
		putOperation(INT, INT, BITWISE_AND, Operations::op_intBITANDint, Operations::fc_intBITANDint);
	}

	/** the address of the first operand for operations closures */
	private static final MemAddress fcOp1 = new MemAddress(Register.RSP, 16);
	/** the address of the second operand for operations closures */
	private static final MemAddress fcOp2 = new MemAddress(Register.RSP, 8);
	/** the address of the operand for single operand operations */
	private static final MemAddress fcSOp = new MemAddress(Register.RSP, 8);
	
	static NativeFunctionWriter fcSimpleFPUOperation(OpCode opCode, int opFPUflags) {
		return is -> {
			Address op1 = fcOp1, op2 = fcOp2;
			
			if((opFPUflags & F_SWAP_ARGS) != 0) {
				Address op = op1;
				op1 = op2;
				op2 = op;
			}
			
			is.addCasted(OpCode.FLD, MemSize.QWORD, op1);
			is.addCasted(OpCode.FLD, MemSize.QWORD, op2);
			is.add(opCode);
			is.addCasted(OpCode.FSTP, MemSize.QWORD, fcOp1);
			is.mov(Register.RAX, fcOp1);
			if((opFPUflags & F_POPAFTER) != 0)
				is.add(OpCode.FSTP);
			is.ret(16);
		};
	}
	
	static OperationWriter opSimpleFPUOperation(OpCode opCode, int opFPUflags) {
		return (e1, e2, asmWriter, errors) -> {
			if((opFPUflags & F_SWAP_ARGS) != 0) {
				Expression e = e1;
				e1 = e2;
				e2 = e;
			}
			OperationParameter ro = asmWriter.prepareRAXRBX(e1, e2, (opFPUflags & F_COMMUTATIVE) != 0, errors);
			MemAddress floatst = asmWriter.writer.unitWriter.requireExternLabel(GlobalLabels.ADDRESS_FLOATST);
			asmWriter.writer.instructions.mov(floatst, Register.RAX);
			asmWriter.writer.instructions.addCasted(OpCode.FLD, MemSize.QWORD, floatst);
			asmWriter.writer.mem.moveData(floatst, ro);
			asmWriter.writer.instructions.addCasted(OpCode.FLD, MemSize.QWORD, floatst);
			asmWriter.writer.instructions.add(opCode);
			asmWriter.writer.instructions.addCasted(OpCode.FSTP, MemSize.QWORD, floatst);
			asmWriter.writer.instructions.mov(Register.RAX, floatst);
			if((opFPUflags & F_POPAFTER) != 0)
				asmWriter.writer.instructions.add(OpCode.FSTP);
		};
	}
	
	static OperationWriter opSimpleCmpOperation(OpCode set) {
		return (leftOperand, rightOperand, asmWriter, errors) -> {
			asmWriter.forcePrepareRAXRBX(leftOperand, rightOperand, errors);
			asmWriter.writer.instructions.cmp(Register.RAX, Register.RBX);
			asmWriter.writer.instructions.mov(Register.RAX, 0);
			asmWriter.writer.instructions.add(OpCode.SETL, Register.AL);
		};
	}
	
	static NativeFunctionWriter fcSimpleCmpOperation(OpCode set) {
		return is -> {
			is.clearRegister(Register.RAX);
			is.mov(Register.RBX, fcOp1);
			is.cmp(Register.RBX, fcOp2);
			is.add(set, Register.AL);
			is.ret(16);
		};
	}
	
	/**
	 * @param allocNumber the number of allocations already made before
	 * 		this allocation call, used to create an error label
	 */
	static Consumer<OperationParameter> fcAllocCall(InstructionSet is, int allocNumber) {
		return allocArg -> {
			String errLabel = ".special_"+allocNumber;
			is.push(allocArg);
			is.call(GlobalLabels.SPECIAL_ALLOC);
			is.test(Register.RAX);
			is.add(OpCode.JNZ, errLabel);
			is.call(GlobalLabels.SPECIAL_THROW);
			is.label(errLabel);
		};
	}
	
	static void op_intADDint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter ro = asmWriter.prepareRAXRBX(leftOperand, rightOperand, true, errors);
		if(ro instanceof IntLiteral) {
			Long v = ((IntLiteral) ro).value;
			if(v == 1)
				asmWriter.writer.instructions.add(OpCode.INC, Register.RAX);
			else if(v == -1)
				asmWriter.writer.instructions.add(OpCode.DEC, Register.RAX);
			else
				asmWriter.writer.instructions.add(OpCode.ADD, Register.RAX, ro);
			return;
		}
		asmWriter.writer.instructions.add(OpCode.ADD, Register.RAX, ro);
	}
	
	static void fc_intADDint(InstructionSet is) {
		is.mov(Register.RAX, fcOp1);
		is.add(OpCode.ADD, Register.RAX, fcOp2);
		is.ret(16);
	}

	static void op_intSUBint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter ro = asmWriter.prepareRAXRBX(leftOperand, rightOperand, false, errors);
		asmWriter.writer.instructions.add(OpCode.SUB, Register.RAX, ro);
	}
	
	static void fc_intSUBint(InstructionSet is) {
		is.mov(Register.RAX, fcOp1);
		is.add(OpCode.SUB, Register.RAX, fcOp2);
		is.ret(16);
	}

	static void op_nullSUBint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		asmWriter.writer.mem.writeTo(Register.RAX, rightOperand, errors);
		asmWriter.writer.instructions.add(OpCode.NEG, Register.RAX);
	}
	
	static void fc_nullSUBint(InstructionSet is) {
		is.mov(Register.RAX, fcSOp);
		is.add(OpCode.NEG, Register.RAX);
		is.ret(8);
	}

	static void op_intMODint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		// TODO check for mod 2^x operations that can be heavily optimized
		asmWriter.forcePrepareRAXRBX(leftOperand, rightOperand, errors);
		asmWriter.writer.instructions.add(OpCode.CQO);
		asmWriter.writer.instructions.add(OpCode.IDIV, Register.RBX);
		asmWriter.writer.instructions.mov(Register.RAX, Register.RDX);
	}
	
	static void fc_intMODint(InstructionSet is) {
		is.mov(Register.RAX, fcOp1);
		is.mov(Register.RBX, fcOp2);
		is.add(OpCode.CQO);
		is.add(OpCode.IDIV, Register.RBX);
		is.mov(Register.RAX, Register.RDX);
		is.ret(16);
	}

	static void op_intMULint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		// TODO check for mod 2^x operations that can be heavily optimized
		OperationParameter ro = asmWriter.prepareRAXRBX(leftOperand, rightOperand, true, errors);
		asmWriter.writer.instructions.add(OpCode.IMUL, Register.RAX, ro);
	}
	
	static void fc_intMULint(InstructionSet is) {
		is.mov(Register.RAX, fcOp1);
		is.add(OpCode.IMUL, Register.RAX, fcOp2);
		is.ret(16);
	}

	static void op_intDIVint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		// TODO check for mod 2^x operations that can be heavily optimized
		asmWriter.forcePrepareRAXRBX(leftOperand, rightOperand, errors);
		asmWriter.writer.instructions.add(OpCode.CQO);
		asmWriter.writer.instructions.add(OpCode.IDIV, Register.RBX);
	}
	
	static void fc_intDIVint(InstructionSet is) {
		is.mov(Register.RAX, fcOp1);
		is.mov(Register.RBX, fcOp2);
		is.add(OpCode.CQO);
		is.add(OpCode.IDIV, Register.RBX);
		is.ret(16);
	}

	static void op_intSHRint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter ro = asmWriter.prepareRAXRBX(leftOperand, rightOperand, false, errors);
		asmWriter.writer.instructions.add(OpCode.SHR, Register.RAX, ro);
	}
	
	static void op_intSHLint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter ro = asmWriter.prepareRAXRBX(leftOperand, rightOperand, false, errors);
		asmWriter.writer.instructions.add(OpCode.SHL, Register.RAX, ro);
	}
	
	static void op_intPOWERint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		if(rightOperand instanceof IntLiteral && ((IntLiteral) rightOperand).value == 2) {
			asmWriter.writer.expWriter.writeExpression(leftOperand, errors);
			asmWriter.writer.instructions.add(OpCode.IMUL, Register.RAX, Register.RAX);
		} else {
			throw new UnimplementedException("unimplemented power operation"); // TODO int and float power operations
			// also repair fc_intPOWERint
		}
	}
	
	static void fc_intPOWERint(InstructionSet is) {
		is.mov(Register.RAX, 99999);
		is.ret(16);
	}

	static void op_nullSUBfloat(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		asmWriter.writer.mem.writeTo(Register.RAX, rightOperand, errors);
		asmWriter.writer.instructions.add(OpCode.XOR, Register.RAX, asmWriter.writer.unitWriter.requireExternLabel(GlobalLabels.ADDRESS_FSIGNBIT));
	}
	
	static void fc_nullSUBfloat(InstructionSet is) {
		is.mov(Register.RAX, fcSOp);
		is.add(OpCode.XOR, Register.RAX, GlobalLabels.ADDRESS_FSIGNBIT);
		is.ret(8);
	}

	static void op_nullNOTbool(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		asmWriter.writer.mem.writeTo(Register.RAX, rightOperand, errors);
		asmWriter.writer.instructions.add(OpCode.XOR, Register.RAX, 1);
	}
	
	static void fc_nullNOTbool(InstructionSet is) {
		is.mov(Register.RAX, fcSOp);
		is.add(OpCode.XOR, Register.RAX, 1);
		is.ret(8);
	}

	static void op_floatPOWERfloat(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		/*
		 * The uncommented code works fine to compute exp(leftOperand)
		 * 
		 * the remaining commented code was an attempt at computing ln(rightOperand)
		 */
		
		OperationParameter ro = asmWriter.prepareRAXRBX(leftOperand, rightOperand, false, errors);
		MemAddress floatst = asmWriter.writer.unitWriter.requireExternLabel(GlobalLabels.ADDRESS_FLOATST);
		asmWriter.writer.instructions.mov(floatst, Register.RAX);
		asmWriter.writer.instructions.addCasted(OpCode.FLD, MemSize.QWORD, floatst);
//		asmWriter.writer.instructions.mov(GlobalLabels.ADDRESS_FLOATST, 1072632447);
//		asmWriter.writer.instructions.addCasted(OpCode.FILD, MemSize.QWORD, GlobalLabels.ADDRESS_FLOATST);
//		asmWriter.writer.instructions.add(OpCode.FSUBP);
//		asmWriter.writer.instructions.mov(GlobalLabels.ADDRESS_FLOATST, 1512775);
//		asmWriter.writer.instructions.addCasted(OpCode.FILD, MemSize.QWORD, GlobalLabels.ADDRESS_FLOATST);
//		asmWriter.writer.instructions.add(OpCode.FDIVP);
//		asmWriter.writer.instructions.addCasted(OpCode.FISTTP, MemSize.QWORD, floatst);
//		asmWriter.writer.instructions.add(OpCode.SHL, Register.RAX, 32);
//		// "long" -> "double"
//		asmWriter.writer.instructions.addCasted(OpCode.FLD, MemSize.QWORD, floatst);
//		asmWriter.writer.mem.moveData(floatst, ro);
//		asmWriter.writer.instructions.addCasted(OpCode.FLD, MemSize.QWORD, floatst);
//		asmWriter.writer.instructions.add(OpCode.FMULP);
		
		// https://stackoverflow.com/questions/48713712/calculating-expx-in-x86-assembly
		asmWriter.writer.instructions.add(new SpecialInstruction("fldl2e"));
		asmWriter.writer.instructions.add(new SpecialInstruction("fmulp st1,st0"));
		asmWriter.writer.instructions.add(new SpecialInstruction("fld1"));
		asmWriter.writer.instructions.add(new SpecialInstruction("fscale"));
		asmWriter.writer.instructions.add(new SpecialInstruction("fxch"));
		asmWriter.writer.instructions.add(new SpecialInstruction("fld1"));
		asmWriter.writer.instructions.add(new SpecialInstruction("fxch"));
		asmWriter.writer.instructions.add(new SpecialInstruction("fprem"));
		asmWriter.writer.instructions.add(new SpecialInstruction("f2xm1"));
		asmWriter.writer.instructions.add(new SpecialInstruction("faddp st1,st0"));
		asmWriter.writer.instructions.add(new SpecialInstruction("fmulp st1,st0"));
		asmWriter.writer.instructions.addCasted(OpCode.FSTP, MemSize.QWORD, floatst);
		asmWriter.writer.instructions.mov(Register.RAX, floatst);
	}
	
	static void fc_floatPOWERfloat(InstructionSet is) {
		fc_intPOWERint(is); // unimplemented as well !!!!
	}

	static void op_strADDstr(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		asmWriter.forcePrepareRAXRBX(leftOperand, rightOperand, errors);
		addStrings(asmWriter.writer.instructions, asmWriter.writer.unitWriter::callAlloc);
	}
	
	static void fc_strADDstr(InstructionSet is) {
		addStrings(is, fcAllocCall(is, 0));
		is.ret(16);
	}
	
	private static void addStrings(InstructionSet is, Consumer<OperationParameter> allocCall) {
		is.mov(Register.RCX, new MemAddress(Register.RAX, -MemSize.POINTER_SIZE));
		is.add(OpCode.ADD, Register.RCX, new MemAddress(Register.RBX, -MemSize.POINTER_SIZE));
		is.push(Register.RBX);
		is.push(Register.RAX);
		allocCall.accept(Register.RCX);
		is.mov(Register.RDI, Register.RAX);
		
		// copy bytes twice
		
		is.pop(Register.RSI); // pop the first string address
		is.mov(Register.RCX, new MemAddress(Register.RSI, -MemSize.POINTER_SIZE));
		is.add(OpCode.CLD);
		is.repeat(OpCode.MOVSB);
		
		is.pop(Register.RSI); // pop the second string address
		is.mov(Register.RCX, new MemAddress(Register.RSI, -MemSize.POINTER_SIZE));
		is.add(OpCode.CLD);
		is.repeat(OpCode.MOVSB);
	}
	
	static void op_universalEquality(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		// TODO find a better way of avoiding 64bits immediate values 
		// a lot of instructions cannot take 64bits immediate values, only 32bits.
		// cmp is one of them, therefore we should move both operand to registers
		// before using cmp
		OperationParameter ro = asmWriter.prepareRAXRBX(leftOperand, rightOperand, true, errors);
		if(ro instanceof ImmediateValue && ((ImmediateValue) ro).text.equals("0")) {
			asmWriter.writer.instructions.test(Register.RAX);
		} else {
			asmWriter.writer.instructions.cmp(Register.RAX, ro);
		}
		
//		asmWriter.forcePrepareRAXRBX(leftOperand, rightOperand, errors);
//		asmWriter.writer.instructions.cmp(Register.RAX, Register.RBX);
		
		asmWriter.writer.instructions.mov(Register.RAX, 0); // clear rax without setting rflags
		asmWriter.writer.instructions.add(OpCode.SETE, Register.AL);
	}
	
	static void fc_universalEquality(InstructionSet is) {
		is.mov(Register.RAX, fcOp1);
		is.cmp(Register.RAX, fcOp2);
		is.mov(Register.RAX, 0); // clear rax without setting rflags
		is.add(OpCode.SETE, Register.AL);
		is.ret(16);
	}

	static void op_universalNEquality(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		// TODO find a better way of avoiding 64bits immediate values 
		// a lot of instructions cannot take 64bits immediate values, only 32bits.
		// cmp is one of them, therefore we should move both operand to registers
		// before using cmp
		OperationParameter ro = asmWriter.prepareRAXRBX(leftOperand, rightOperand, true, errors);
		if(ro instanceof ImmediateValue && ((ImmediateValue) ro).text.equals("0")) {
			asmWriter.writer.instructions.test(Register.RAX);
		} else {
			asmWriter.writer.instructions.cmp(Register.RAX, ro);
		}
		
//		asmWriter.forcePrepareRAXRBX(leftOperand, rightOperand, errors);
//		asmWriter.writer.instructions.cmp(Register.RAX, Register.RBX);
		
		asmWriter.writer.instructions.mov(Register.RAX, 0); // clear rax without setting rflags
		asmWriter.writer.instructions.add(OpCode.SETNE, Register.AL);
	}
	
	static void fc_universalNEquality(InstructionSet is) {
		is.mov(Register.RAX, fcOp1);
		is.cmp(Register.RAX, fcOp2);
		is.mov(Register.RAX, 0); // clear rax without setting rflags
		is.add(OpCode.SETNE, Register.AL);
		is.ret(16);
	}
	
	static void op_boolORbool(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		String specialLabel = asmWriter.writer.unitWriter.getSpecialLabel();
		asmWriter.writer.expWriter.writeExpression(leftOperand, errors);
		asmWriter.writer.instructions.test(Register.RAX);
		asmWriter.writer.instructions.add(OpCode.JNZ, specialLabel);
		asmWriter.writer.expWriter.writeExpression(rightOperand, errors);
		asmWriter.writer.instructions.label(specialLabel);
	}
	
	static void fc_boolORbool(InstructionSet is) {
		is.mov(Register.RAX, fcOp1);
		is.add(OpCode.OR, Register.RAX, fcOp2);
		is.ret(16);
	}
	
	static void op_boolANDbool(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		String specialLabel = asmWriter.writer.unitWriter.getSpecialLabel();
		asmWriter.writer.expWriter.writeExpression(leftOperand, errors);
		asmWriter.writer.instructions.test(Register.RAX);
		asmWriter.writer.instructions.add(OpCode.JZ, specialLabel);
		asmWriter.writer.expWriter.writeExpression(rightOperand, errors);
		asmWriter.writer.instructions.label(specialLabel);
	}
	
	static void fc_boolANDbool(InstructionSet is) {
		is.mov(Register.RAX, fcOp1);
		is.add(OpCode.AND, Register.RAX, fcOp2);
		is.ret(16);
	}
	
	static void op_intBITORint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		asmWriter.forcePrepareRAXRBX(leftOperand, rightOperand, errors);
		asmWriter.writer.instructions.add(OpCode.OR, Register.RAX, Register.RBX);
	}
	
	static void fc_intBITORint(InstructionSet is) {
		is.mov(Register.RAX, fcOp1);
		is.add(OpCode.OR, Register.RAX, fcOp2);
		is.ret(16);
	}
	
	static void op_intBITANDint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		asmWriter.forcePrepareRAXRBX(leftOperand, rightOperand, errors);
		asmWriter.writer.instructions.add(OpCode.AND, Register.RAX, Register.RBX);
	}
	
	static void fc_intBITANDint(InstructionSet is) {
		is.mov(Register.RAX, fcOp1);
		is.add(OpCode.AND, Register.RAX, fcOp2);
		is.ret(16);
	}
	
}
