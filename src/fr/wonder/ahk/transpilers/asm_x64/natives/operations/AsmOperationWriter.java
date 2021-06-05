package fr.wonder.ahk.transpilers.asm_x64.natives.operations;

import static fr.wonder.ahk.compiled.expressions.Operator.*;
import static fr.wonder.ahk.compiled.expressions.types.VarType.INT;
import static fr.wonder.ahk.compiler.types.NativeOperation.get;

import java.util.HashMap;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.types.Operation;
import fr.wonder.ahk.transpilers.asm_x64.writers.UnitWriter;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.ImmediateValue;
import fr.wonder.ahk.transpilers.common_x64.addresses.LabelAddress;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.ahk.transpilers.common_x64.instructions.OperationParameter;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnimplementedException;
import fr.wonder.commons.types.Tuple;

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
		nativeOperations.put(get(INT, INT, ADD), AsmOperationWriter::op_intADDint);
		nativeOperations.put(get(INT, INT, MULTIPLY), AsmOperationWriter::op_intMULint);
		nativeOperations.put(get(INT, INT, DIVIDE), AsmOperationWriter::op_intDIVint);
		nativeOperations.put(get(INT, INT, MOD), AsmOperationWriter::op_intMODint);
	}
	
	static {
		conditionalJumps.put(get(INT, INT, EQUALS), AsmOperationWriter::jump_intEQUint);
		conditionalJumps.put(get(INT, INT, NEQUALS), AsmOperationWriter::jump_intNEQUint);
		conditionalJumps.put(get(INT, INT, LOWER), AsmOperationWriter::jump_intLTint);
	}
	
	static {
		conversions.put(new Tuple<>(VarType.INT, VarType.FLOAT), AsmOperationWriter::conv_intTOfloat);
		conversions.put(new Tuple<>(VarType.FLOAT, VarType.INT), AsmOperationWriter::conv_floatTOint);
	}
	
	private final UnitWriter writer;
	
	public AsmOperationWriter(UnitWriter writer) {
		this.writer = writer;
	}
	
	/* ============================================ Operations ============================================ */
	
	public boolean writeOperation(Operation op, Expression leftOperand, Expression rightOperand, ErrorWrapper errors) {
		OperationWriter opw = nativeOperations.get(op);
		if(opw == null)
			return false;
		opw.write(leftOperand, rightOperand, this, errors);
		return true;
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
			return new ImmediateValue(((LiteralExp<?>) e2).toString());
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
//		asmWriter.writer.buffer.writeLine("; conv");
		throw new UnimplementedException();
	}
	
	private static void conv_floatTOint(VarType from, VarType to, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		throw new UnimplementedException();
//		asmWriter.writer.buffer.writeLine("mov ["+floatst+"],rax"); // floatst -> GlobalLabels.floatst
//		asmWriter.writer.buffer.writeLine("fld qword["+floatst+"]");
//		asmWriter.writer.buffer.writeLine("fistp qword["+floatst+"]");
//		asmWriter.writer.buffer.writeLine("mov rax,["+floatst+"]");
	}
	
}
