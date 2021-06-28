package fr.wonder.ahk.transpilers.asm_x64.natives.operations;

import static fr.wonder.ahk.compiled.expressions.Operator.ADD;
import static fr.wonder.ahk.compiled.expressions.Operator.DIVIDE;
import static fr.wonder.ahk.compiled.expressions.Operator.EQUALS;
import static fr.wonder.ahk.compiled.expressions.Operator.LOWER;
import static fr.wonder.ahk.compiled.expressions.Operator.MOD;
import static fr.wonder.ahk.compiled.expressions.Operator.MULTIPLY;
import static fr.wonder.ahk.compiled.expressions.Operator.NEQUALS;
import static fr.wonder.ahk.compiled.expressions.Operator.SUBSTRACT;
import static fr.wonder.ahk.compiled.expressions.types.VarType.FLOAT;
import static fr.wonder.ahk.compiled.expressions.types.VarType.INT;

import java.util.HashMap;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.FloatLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.types.NativeOperation;
import fr.wonder.ahk.compiler.types.Operation;
import fr.wonder.ahk.transpilers.asm_x64.writers.UnitWriter;
import fr.wonder.ahk.transpilers.common_x64.GlobalLabels;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.ImmediateValue;
import fr.wonder.ahk.transpilers.common_x64.addresses.LabelAddress;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.ahk.transpilers.common_x64.instructions.OperationParameter;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.types.Tuple;
import fr.wonder.commons.utils.Assertions;

public class AsmOperationWriter {
	
	private static final Map<Operation, OperationWriter> nativeOperations = new HashMap<>();
	private static final Map<Operation, JumpWriter> conditionalJumps = new HashMap<>();
	private static final Map<Tuple<VarType, VarType>, ConversionWriter> conversions = new HashMap<>();
	
	private static interface OperationWriter {
		
		void write(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors);
		
	}
	
	private static interface JumpWriter {
		
		void write(OperationExp condition, String label, AsmOperationWriter asmWriter, ErrorWrapper errors);
		
	}
	
	private static interface ConversionWriter {
		
		void write(VarType from, VarType to, AsmOperationWriter asmWriter, ErrorWrapper errors);
		
	}
	
	static {
		nativeOperations.put(NativeOperation.getOperation(INT, INT, ADD, false), AsmOperationWriter::op_intADDint);
		nativeOperations.put(NativeOperation.getOperation(INT, INT, SUBSTRACT, false), AsmOperationWriter::op_intSUBint);
		nativeOperations.put(NativeOperation.getOperation(null, INT, SUBSTRACT, false), AsmOperationWriter::op_nullSUBint);
		nativeOperations.put(NativeOperation.getOperation(INT, INT, MULTIPLY, false), AsmOperationWriter::op_intMULint);
		nativeOperations.put(NativeOperation.getOperation(INT, INT, DIVIDE, false), AsmOperationWriter::op_intDIVint);
		nativeOperations.put(NativeOperation.getOperation(INT, INT, MOD, false), AsmOperationWriter::op_intMODint);
		
		nativeOperations.put(NativeOperation.getOperation(FLOAT, FLOAT, ADD, false), AsmOperationWriter::op_floatADDfloat);
		nativeOperations.put(NativeOperation.getOperation(FLOAT, FLOAT, SUBSTRACT, false), AsmOperationWriter::op_floatSUBfloat);
		nativeOperations.put(NativeOperation.getOperation(null, FLOAT, SUBSTRACT, false), AsmOperationWriter::op_nullSUBfloat);
		
		Assertions.assertNull(nativeOperations.get(null), "An unimplemented native operation was given an asm implementation");
	}
	
	static {
		conditionalJumps.put(NativeOperation.getOperation(INT, INT, EQUALS, false), AsmOperationWriter::jump_intEQUint);
		conditionalJumps.put(NativeOperation.getOperation(INT, INT, NEQUALS, false), AsmOperationWriter::jump_intNEQUint);
		conditionalJumps.put(NativeOperation.getOperation(INT, INT, LOWER, false), AsmOperationWriter::jump_intLTint);
		
		Assertions.assertNull(conditionalJumps.get(null), "An unimplemented native operation was given an asm jump implementation");
	}
	
	static {
		conversions.put(new Tuple<>(VarType.INT, VarType.FLOAT), AsmOperationWriter::conv_intTOfloat);
		conversions.put(new Tuple<>(VarType.FLOAT, VarType.INT), AsmOperationWriter::conv_floatTOint);
		conversions.put(new Tuple<>(VarType.BOOL, VarType.INT), (from, to, writer, errors) -> {}); // NOOP
	}
	
	private final UnitWriter writer;
	
	public AsmOperationWriter(UnitWriter writer) {
		this.writer = writer;
	}
	
	/* ============================================ Operations ============================================ */
	
	public void writeOperation(OperationExp exp, ErrorWrapper errors) {
		OperationWriter opw = nativeOperations.get(exp.getOperation());
		if(opw == null)
			errors.add("Unimplemented assembly operation! " + exp.operationString() + exp.getErr());
		else
			opw.write(exp.getLeftOperand(), exp.getRightOperand(), this, errors);
	}
	
	/**
	 * Writes {@code e1} and {@code e2} to {@code rax, rbx} if they're computable expressions,
	 * if one of them is a literal expression only the other one is placed into {@code rax}
	 * and the other one is returned as an {@link ImmediateValue}, otherwise {@code Register.RBX}
	 * is returned.
	 */
	private OperationParameter prepareRAXRBX(Expression e1, Expression e2, boolean commutativeOperation, ErrorWrapper errors) {
		if(commutativeOperation && e1 instanceof LiteralExp && !(e2 instanceof LiteralExp)) {
			Expression t = e2;
			e2 = e1;
			e1 = t;
		}
		writer.mem.writeTo(Register.RAX, e1, errors);
		if(e2 instanceof LiteralExp) {
			return new ImmediateValue(writer.getValueString((LiteralExp<?>) e2));
		} else if(e2 instanceof VarExp) {
			return writer.mem.getVarAddress(((VarExp) e2).declaration);
		} else {
			writer.mem.addStackOffset(8);
			writer.instructions.push(Register.RAX);
			writer.mem.writeTo(Register.RBX, e2, errors);
			writer.mem.addStackOffset(-8);
			writer.instructions.pop(Register.RAX);
			return Register.RBX;
		}
	}
	
	private void forcePrepareRAXRBX(Expression e1, Expression e2, ErrorWrapper errors) {
		writer.mem.writeTo(Register.RAX, e1, errors);
		writer.mem.addStackOffset(8);
		writer.instructions.push(Register.RAX);
		writer.mem.writeTo(Register.RBX, e2, errors);
		writer.mem.addStackOffset(-8);
		writer.instructions.pop(Register.RAX);
	}
	
	private static void op_intADDint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
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
	
	private static void op_intSUBint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter ro = asmWriter.prepareRAXRBX(leftOperand, rightOperand, false, errors);
		asmWriter.writer.instructions.add(OpCode.SUB, Register.RAX, ro);
	}
	
	private static void op_nullSUBint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		asmWriter.writer.mem.writeTo(Register.RAX, rightOperand, errors);
		asmWriter.writer.instructions.add(OpCode.NEG, Register.RAX);
	}
	
	private static void op_intMODint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		// TODO check for mod 2^x operations that can be heavily optimized
		asmWriter.forcePrepareRAXRBX(leftOperand, rightOperand, errors);
		asmWriter.writer.instructions.add(OpCode.CQO);
		asmWriter.writer.instructions.add(OpCode.IDIV, Register.RBX);
		asmWriter.writer.instructions.mov(Register.RAX, Register.RDX);
	}

	private static void op_intMULint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		// TODO check for mod 2^x operations that can be heavily optimized
		OperationParameter ro = asmWriter.prepareRAXRBX(leftOperand, rightOperand, true, errors);
		asmWriter.writer.instructions.add(OpCode.IMUL, Register.RAX, ro);
	}
	
	private static void op_intDIVint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		// TODO check for mod 2^x operations that can be heavily optimized
		asmWriter.forcePrepareRAXRBX(leftOperand, rightOperand, errors);
//		asmWriter.writer.instructions.clearRegister(Register.RDX);
		asmWriter.writer.instructions.add(OpCode.CQO);
		asmWriter.writer.instructions.add(OpCode.IDIV, Register.RBX);
	}
	
	private static void op_floatADDfloat(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter ro = asmWriter.prepareRAXRBX(leftOperand, rightOperand, true, errors);
		asmWriter.writer.instructions.mov(GlobalLabels.ADDRESS_FLOATST, Register.RAX);
		asmWriter.writer.instructions.addCasted(OpCode.FLD, MemSize.QWORD, GlobalLabels.ADDRESS_FLOATST);
		if(ro instanceof FloatLiteral && ((FloatLiteral) ro).value == 1) {
			asmWriter.writer.instructions.add(OpCode.FLD1);
		} else {
			asmWriter.writer.mem.moveData(GlobalLabels.ADDRESS_FLOATST, ro, MemSize.QWORD);
			asmWriter.writer.instructions.addCasted(OpCode.FLD, MemSize.QWORD, GlobalLabels.ADDRESS_FLOATST);
		}
		asmWriter.writer.instructions.add(OpCode.FADDP);
		asmWriter.writer.instructions.addCasted(OpCode.FSTP, MemSize.QWORD, GlobalLabels.ADDRESS_FLOATST);
		asmWriter.writer.instructions.mov(Register.RAX, GlobalLabels.ADDRESS_FLOATST);
	}
	
	private static void op_floatSUBfloat(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter ro = asmWriter.prepareRAXRBX(leftOperand, rightOperand, false, errors);
		asmWriter.writer.instructions.mov(GlobalLabels.ADDRESS_FLOATST, Register.RAX);
		asmWriter.writer.instructions.addCasted(OpCode.FLD, MemSize.QWORD, GlobalLabels.ADDRESS_FLOATST);
		if(ro instanceof FloatLiteral && ((FloatLiteral) ro).value == 1) {
			asmWriter.writer.instructions.add(OpCode.FLD1);
		} else {
			asmWriter.writer.mem.moveData(GlobalLabels.ADDRESS_FLOATST, ro, MemSize.QWORD);
			asmWriter.writer.instructions.addCasted(OpCode.FLD, MemSize.QWORD, GlobalLabels.ADDRESS_FLOATST);
		}
		asmWriter.writer.instructions.add(OpCode.FSUBP);
		asmWriter.writer.instructions.addCasted(OpCode.FSTP, MemSize.QWORD, GlobalLabels.ADDRESS_FLOATST);
		asmWriter.writer.instructions.mov(Register.RAX, GlobalLabels.ADDRESS_FLOATST);
	}
	
	private static void op_nullSUBfloat(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		asmWriter.writer.mem.writeTo(Register.RAX, rightOperand, errors);
		asmWriter.writer.instructions.add(OpCode.XOR, Register.RAX, GlobalLabels.ADDRESS_VAL_FSIGNBIT);
	}
	
	/* =============================================== Jumps ============================================== */
	
	/**
	 * writes the 2/3 letters jump instruction (ie: jmp/je/jge...),
	 * returns true if the condition has a specific jump instruction, false otherwise
	 * @param label the label to jump to if the condition is <b>NOT</b> met<br>
	 *     e.g the label of the section end for an if-then statement for example
	 */
	public void writeJump(Expression condition, String label, ErrorWrapper errors) {
		if(condition instanceof OperationExp) {
			OperationExp oc = (OperationExp) condition;
			JumpWriter jump = conditionalJumps.get(oc.getOperation());
			if(jump != null) {
				jump.write(oc, label, this, errors);
				return;
			}
		}
		writer.expWriter.writeExpression(condition, errors);
		writer.instructions.test(Register.RAX);
		writer.instructions.add(OpCode.JZ, label);
	}
	
	private static void jump_intEQUint(OperationExp exp, String label, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter rv = asmWriter.prepareRAXRBX(exp.getLeftOperand(), exp.getRightOperand(), true, errors);
		if(rv instanceof ImmediateValue && ((ImmediateValue)rv).text.equals("0"))
			asmWriter.writer.instructions.test(Register.RAX);
		else
			asmWriter.writer.instructions.cmp(Register.RAX, rv);
		asmWriter.writer.instructions.add(OpCode.JNE, new LabelAddress(label));
	}
	
	private static void jump_intNEQUint(OperationExp exp, String label, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter rv = asmWriter.prepareRAXRBX(exp.getLeftOperand(), exp.getRightOperand(), true, errors);
		if(rv instanceof ImmediateValue && ((ImmediateValue)rv).text.equals("0"))
			asmWriter.writer.instructions.test(Register.RAX);
		else
			asmWriter.writer.instructions.cmp(Register.RAX, rv);
		asmWriter.writer.instructions.add(OpCode.JE, new LabelAddress(label));
	}
	
	private static void jump_intLTint(OperationExp exp, String label, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter rv = asmWriter.prepareRAXRBX(exp.getLeftOperand(), exp.getRightOperand(), false, errors);
		asmWriter.writer.instructions.cmp(Register.RAX, rv);
		asmWriter.writer.instructions.add(OpCode.JGE, new LabelAddress(label));
	}

	/* ============================================ Conversions =========================================== */
	
	public void writeConversion(VarType from, VarType to, ErrorWrapper errors) {
		ConversionWriter cw = conversions.get(new Tuple<>(from, to));
		if(cw == null)
			throw new IllegalArgumentException("Unimplemented conversion " + from + " -> " + to);
		cw.write(from, to, this, errors);
	}
	
	private static void conv_intTOfloat(VarType from, VarType to, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		asmWriter.writer.instructions.mov(GlobalLabels.ADDRESS_FLOATST, Register.RAX);
		asmWriter.writer.instructions.addCasted(OpCode.FILD, MemSize.QWORD, GlobalLabels.ADDRESS_FLOATST);
		asmWriter.writer.instructions.addCasted(OpCode.FSTP, MemSize.QWORD, GlobalLabels.ADDRESS_FLOATST);
		asmWriter.writer.instructions.mov(Register.RAX, GlobalLabels.ADDRESS_FLOATST);
	}
	
	private static void conv_floatTOint(VarType from, VarType to, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		asmWriter.writer.instructions.mov(GlobalLabels.ADDRESS_FLOATST, Register.RAX);
		asmWriter.writer.instructions.addCasted(OpCode.FLD, MemSize.QWORD, GlobalLabels.ADDRESS_FLOATST);
		asmWriter.writer.instructions.addCasted(OpCode.FISTP, MemSize.QWORD, GlobalLabels.ADDRESS_FLOATST);
		asmWriter.writer.instructions.mov(Register.RAX, GlobalLabels.ADDRESS_FLOATST);
	}
	
}
