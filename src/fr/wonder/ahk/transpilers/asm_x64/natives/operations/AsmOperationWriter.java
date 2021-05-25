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
		nativeOperations.put(get(INT, INT, MOD), AsmOperationWriter::op_intMODint);
		nativeOperations.put(get(INT, INT, DIVIDE), AsmOperationWriter::op_intDIVint);
	}
	
	static {
		conditionalJumps.put(get(INT, INT, EQUALS), AsmOperationWriter::jump_intEQUint);
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
	private OperationParameter prepareRAXRBX(Expression e1, Expression e2, ErrorWrapper errors) {
		// FIX (3 % i) will be interpreted as (i % 3) currently!!
		if(e1 instanceof LiteralExp && !(e2 instanceof LiteralExp)) {
			Expression t = e2;
			e2 = e1;
			e1 = t;
		}
		writer.mem.writeTo(Register.RAX, e1, errors);
		writer.mem.addStackOffset(8);
		writer.instructions.push(Register.RAX);
		OperationParameter acc = writer.mem.moveTo(Register.RBX, e2, errors);
		writer.mem.restoreStackOffset();
		writer.instructions.pop(Register.RAX);
		return acc;
	}
	
	private static void op_intADDint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter ro = asmWriter.prepareRAXRBX(leftOperand, rightOperand, errors);
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
		asmWriter.writer.mem.writeTo(Register.RAX, leftOperand, errors);
		asmWriter.writer.mem.writeTo(Register.RBX, rightOperand, errors); // FIX rax may be overwritten by the calculation
		asmWriter.writer.instructions.clearRegister(Register.RDX);
		asmWriter.writer.instructions.add(OpCode.IDIV, Register.RBX);
		asmWriter.writer.instructions.mov(Register.RDX, Register.RAX);
	}
	
	private static void op_intDIVint(Expression leftOperand, Expression rightOperand, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		// TODO check for mod 2^x operations that can be heavily optimized
		asmWriter.writer.mem.writeTo(Register.RAX, leftOperand, errors);
		asmWriter.writer.mem.writeTo(Register.RBX, rightOperand, errors); // FIX rax may be overwritten by the calculation
		asmWriter.writer.instructions.clearRegister(Register.RDX);
		asmWriter.writer.instructions.add(OpCode.IDIV, Register.RBX);
	}
	
	/* =============================================== Jumps ============================================== */
	
	/**
	 * writes the 2/3 letters jump instruction (ie: jmp/je/jge...),
	 * returns true if the condition has a specific jump instruction, false otherwise
	 * @param label the label to jump to if the condition is <b>NOT</b> met<br>
	 *     e.g the label of the section end for an if-then statement for example
	 */
	public boolean writeJump(Expression condition, String label, ErrorWrapper errors) {
		if(condition instanceof OperationExp) {
			OperationExp oc = (OperationExp) condition;
			JumpWriter jump = conditionalJumps.get(oc.getOperation());
			if(jump != null) {
				jump.write(oc, label, this, errors);
				return true;
			}
		}
		return false;
	}
	
	private static void jump_intEQUint(OperationExp exp, String label, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter rv = asmWriter.prepareRAXRBX(exp.getLeftOperand(), exp.getRightOperand(), errors);
		if(rv instanceof IntLiteral && ((IntLiteral)rv).value == 0)
			asmWriter.writer.instructions.test(Register.RAX);
		else
			asmWriter.writer.instructions.cmp(Register.RAX, rv);
		asmWriter.writer.instructions.add(OpCode.JNE, new LabelAddress(label));
	}
	
	private static void jump_intLTint(OperationExp exp, String label, AsmOperationWriter asmWriter, ErrorWrapper errors) {
		OperationParameter rv = asmWriter.prepareRAXRBX(exp.getLeftOperand(), exp.getRightOperand(), errors);
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
