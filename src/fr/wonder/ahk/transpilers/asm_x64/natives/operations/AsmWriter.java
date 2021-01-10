package fr.wonder.ahk.transpilers.asm_x64.natives.operations;

import java.util.HashMap;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiler.types.Operation;
import fr.wonder.ahk.transpilers.asm_x64.writers.UnitWriter;
import fr.wonder.ahk.transpilers.asm_x64.writers.memory.DataAccess;
import fr.wonder.ahk.transpilers.asm_x64.writers.memory.DirectLoc;
import fr.wonder.ahk.utils.ErrorWrapper;
import fr.wonder.commons.types.Tuple;

import static fr.wonder.ahk.compiler.types.NativeOperation.get;
import static fr.wonder.ahk.compiled.expressions.Operator.*;
import static fr.wonder.ahk.compiled.expressions.types.VarType.*;

public class AsmWriter {
	
	private static final Map<Operation, OperationWriter> nativeOperations = new HashMap<>();
	private static final Map<Operation, JumpWriter> conditionalJumps = new HashMap<>();
	private static final Map<Tuple<VarType, VarType>, ConversionWriter> conversions = new HashMap<>();
	
	private final static String floatst = UnitWriter.GLOBAL_FLOATST;
	
	private static interface OperationWriter {
		
		void write(Expression leftOperand, Expression rightOperand, AsmWriter asmWriter, ErrorWrapper errors);
		
	}
	
	private static interface JumpWriter {
		
		void write(OperationExp condition, String label, AsmWriter asmWriter, ErrorWrapper errors);
		
	}
	
	private static interface ConversionWriter {
		
		void write(VarType from, VarType to, AsmWriter asmWriter, ErrorWrapper errors);
		
	}
	
	static {
		nativeOperations.put(get(INT, INT, ADD), AsmWriter::op_intADDint);
		nativeOperations.put(get(INT, INT, MOD), AsmWriter::op_intMODint);
		nativeOperations.put(get(INT, INT, DIVIDE), AsmWriter::op_intDIVint);
	}
	
	static {
		conditionalJumps.put(get(INT, INT, EQUALS), AsmWriter::jump_intEQUint);
		conditionalJumps.put(get(INT, INT, LOWER), AsmWriter::jump_intLTint);
	}
	
	static {
		conversions.put(new Tuple<>(VarType.INT, VarType.FLOAT), AsmWriter::conv_intTOfloat);
		conversions.put(new Tuple<>(VarType.FLOAT, VarType.INT), AsmWriter::conv_floatTOint);
	}
	
	private final UnitWriter writer;
	
	public AsmWriter(UnitWriter writer) {
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
	
	private DataAccess prepareRAXRBX(Expression e1, Expression e2, ErrorWrapper errors) {
		if(e1 instanceof LiteralExp && !(e2 instanceof LiteralExp)) {
			Expression t = e2;
			e2 = e1;
			e1 = t;
		}
		writer.mem.writeTo(DirectLoc.LOC_RAX, e1, errors);
		writer.mem.addStackOffset(8);
		writer.buffer.writeLine("push rax");
		DataAccess acc = writer.mem.moveTo(DirectLoc.LOC_RBX, e2, errors);
		writer.mem.restoreStackOffset();
		writer.buffer.writeLine("pop rax");
		return acc;
	}
	
	private static void op_intADDint(Expression leftOperand, Expression rightOperand, AsmWriter asmWriter, ErrorWrapper errors) {
		DataAccess ro = asmWriter.prepareRAXRBX(leftOperand, rightOperand, errors);
		if(ro.exp != null && ro.exp instanceof IntLiteral && (Long)ro.exp.value == 1)
			asmWriter.writer.buffer.writeLine("inc rax");
		else if(ro.exp != null && ro.exp instanceof IntLiteral && (Long)ro.exp.value == -1)
			asmWriter.writer.buffer.writeLine("dec rax");
		else
			asmWriter.writer.buffer.writeLine("add rax," + ro);
	}
	
	private static void op_intMODint(Expression leftOperand, Expression rightOperand, AsmWriter asmWriter, ErrorWrapper errors) {
		DataAccess ro = asmWriter.prepareRAXRBX(leftOperand, rightOperand, errors);
		// TODO check for mod 2^x operations that can be heavily optimized
		asmWriter.writer.mem.writeTo(DirectLoc.LOC_RBX, ro, errors); // FIX rax may be overridden by ro
		asmWriter.writer.buffer.writeLine("xor rdx,rdx");
		asmWriter.writer.buffer.writeLine("idiv rbx");
		asmWriter.writer.buffer.writeLine("mov rax,rdx");
	}
	
	private static void op_intDIVint(Expression leftOperand, Expression rightOperand, AsmWriter asmWriter, ErrorWrapper errors) {
		DataAccess ro = asmWriter.prepareRAXRBX(leftOperand, rightOperand, errors);
		// TODO check for mod 2^x operations that can be heavily optimized
		asmWriter.writer.mem.writeTo(DirectLoc.LOC_RBX, ro, errors);
		asmWriter.writer.buffer.writeLine("xor rdx,rdx");
		asmWriter.writer.buffer.writeLine("idiv rbx");
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
			JumpWriter jump = conditionalJumps.get(oc.operation);
			if(jump != null) {
				jump.write(oc, label, this, errors);
				return true;
			}
		}
		return false;
	}
	
	private static void jump_intEQUint(OperationExp exp, String label, AsmWriter asmWriter, ErrorWrapper errors) {
		DataAccess rv = asmWriter.prepareRAXRBX(exp.getLeftOperand(), exp.getRightOperand(), errors);
		if(rv.exp != null && rv.exp instanceof IntLiteral && (Long) rv.exp.value == 0)
			asmWriter.writer.buffer.writeLine("test rax,rax");
		else
			asmWriter.writer.buffer.writeLine("cmp rax,"+rv);
		asmWriter.writer.buffer.writeLine("jne "+label);
	}
	
	private static void jump_intLTint(OperationExp exp, String label, AsmWriter asmWriter, ErrorWrapper errors) {
		DataAccess rv = asmWriter.prepareRAXRBX(exp.getLeftOperand(), exp.getRightOperand(), errors);
		asmWriter.writer.buffer.writeLine("cmp rax,"+rv);
		asmWriter.writer.buffer.writeLine("jge "+label);
	}

	/* ============================================ Conversions =========================================== */
	
	public void writeConversion(VarType from, VarType to, ErrorWrapper errors) {
		ConversionWriter cw = conversions.get(new Tuple<>(from, to));
		if(cw == null)
			throw new IllegalArgumentException("Unimplemented conversion " + from + " -> " + to);
		cw.write(from, to, this, errors);
	}
	
	private static void conv_intTOfloat(VarType from, VarType to, AsmWriter asmWriter, ErrorWrapper errors) {
		asmWriter.writer.buffer.writeLine("; conv");
	}
	
	private static void conv_floatTOint(VarType from, VarType to, AsmWriter asmWriter, ErrorWrapper errors) {
		asmWriter.writer.buffer.writeLine("mov ["+floatst+"],rax");
		asmWriter.writer.buffer.writeLine("fld qword["+floatst+"]");
		asmWriter.writer.buffer.writeLine("fistp qword["+floatst+"]");
		asmWriter.writer.buffer.writeLine("mov rax,["+floatst+"]");
	}
	
}
