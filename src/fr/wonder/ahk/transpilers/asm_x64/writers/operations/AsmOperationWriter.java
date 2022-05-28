package fr.wonder.ahk.transpilers.asm_x64.writers.operations;

import java.util.Map.Entry;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.sections.LambdaClosureArgument;
import fr.wonder.ahk.compiler.types.NativeOperation;
import fr.wonder.ahk.transpilers.asm_x64.writers.AbstractWriter;
import fr.wonder.ahk.transpilers.asm_x64.writers.RegistryManager;
import fr.wonder.ahk.transpilers.asm_x64.writers.operations.Conversions.ConversionFunctionWriter;
import fr.wonder.ahk.transpilers.asm_x64.writers.operations.Conversions.ConversionWriter;
import fr.wonder.ahk.transpilers.asm_x64.writers.operations.Jumps.JumpWriter;
import fr.wonder.ahk.transpilers.asm_x64.writers.operations.Operations.NativeFunctionWriter;
import fr.wonder.ahk.transpilers.asm_x64.writers.operations.Operations.OperationWriter;
import fr.wonder.ahk.transpilers.common_x64.InstructionSet;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.ImmediateValue;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.ahk.transpilers.common_x64.instructions.OperationParameter;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.types.Tuple;

public class AsmOperationWriter {
	
	final AbstractWriter writer;
	
	public AsmOperationWriter(AbstractWriter writer) {
		this.writer = writer;
	}

	public static Tuple<String, String> writeOperationsAsClosures() {
		StringBuilder globalLabels = new StringBuilder();
		InstructionSet instructions = new InstructionSet();
		instructions.skip(); // begin by an empty line
		
		for(Entry<NativeOperation, NativeFunctionWriter> writer : Operations.nativeFunctions.entrySet()) {
			String label = RegistryManager.getOperationClosureRegistry(writer.getKey());
			globalLabels.append("global " + label + "\n");
			instructions.label(label);
			writer.getValue().write(instructions);
			instructions.skip();
		}
		
		return new Tuple<>(globalLabels.toString(), instructions.toString());
	}

	public static Tuple<String, String> writeConversionsAsClosures() {
		StringBuilder globalLabels = new StringBuilder();
		InstructionSet instructions = new InstructionSet();
		instructions.skip(); // begin by an empty line
		
		for(Entry<NativeConversion, ConversionFunctionWriter> writer : Conversions.conversionFunctions.entrySet()) {
			String label = RegistryManager.getConversionsClosureRegistry(writer.getKey());
			globalLabels.append("global " + label + "\n");
			instructions.label(label);
			writer.getValue().write(instructions);
			instructions.skip();
		}
		
		return new Tuple<>(globalLabels.toString(), instructions.toString());
	}
	
	public static boolean operationCanBeClosure(NativeOperation op) {
		return Operations.nativeFunctions.containsKey(op);
	}
	
	/* ============================================ Operations ============================================ */
	
	public void writeOperation(OperationExp exp, ErrorWrapper errors) {
		OperationWriter opw = Operations.getOperationWriter(exp.getOperation());
		if(opw == null) {
			errors.add("Unimplemented assembly operation! " + exp.operationString() + exp.getErr());
			return;
		}
		opw.write(exp.getLeftOperand(), exp.getRightOperand(), this, errors);
	}
	
	/**
	 * Writes {@code e1} and {@code e2} to {@code rax, rbx} if they're computable expressions,
	 * if one of them is a literal expression only the other one is placed into {@code rax}
	 * and the other one is returned as an {@link ImmediateValue}, otherwise {@code Register.RBX}
	 * is returned.
	 */
	OperationParameter prepareRAXRBX(Expression e1, Expression e2, boolean commutativeOperation, ErrorWrapper errors) {
		if(commutativeOperation && e1 instanceof LiteralExp && !(e2 instanceof LiteralExp)) {
			Expression t = e2;
			e2 = e1;
			e1 = t;
		}
		writer.mem.writeTo(Register.RAX, e1, errors);
		if(e2 instanceof LiteralExp) {
			return new ImmediateValue(writer.unitWriter.getValueString((LiteralExp<?>) e2));
		} else if(e2 instanceof VarExp) {
			VarAccess access = ((VarExp) e2).declaration;
			if(access instanceof LambdaClosureArgument) {
				MemAddress inClosureAddress = (MemAddress) writer.mem.getVarAddress(access);
				writer.instructions.mov(Register.RBX, inClosureAddress.base);
				writer.instructions.mov(Register.RBX, inClosureAddress.changeBase(Register.RBX));
				return Register.RBX;
			} else {
				// imediate variables or function arguments are imediately accessible on the stack
				return writer.mem.getVarAddress(access);
			}
		} else {
			writer.mem.addStackOffset(8);
			writer.instructions.push(Register.RAX);
			writer.mem.writeTo(Register.RBX, e2, errors);
			writer.mem.addStackOffset(-8);
			writer.instructions.pop(Register.RAX);
			return Register.RBX;
		}
	}
	
	void forcePrepareRAXRBX(Expression e1, Expression e2, ErrorWrapper errors) {
		writer.mem.writeTo(Register.RAX, e1, errors);
		writer.mem.addStackOffset(8);
		writer.instructions.push(Register.RAX);
		writer.mem.writeTo(Register.RBX, e2, errors);
		writer.mem.addStackOffset(-8);
		writer.instructions.pop(Register.RAX);
	}
	
	/* =============================================== Jumps ============================================== */
	
	/**
	 * writes the 2/3 letters jump instruction (ie: jmp/je/jge...)
	 * 
	 * @param jumpIfMet if true, the jump will be done iff the condition is met,
	 * 			otherwise the condition is inverted first.
	 */
	public void writeJump(Expression condition, String label, boolean jumpIfMet, ErrorWrapper errors) {
		if(condition instanceof OperationExp && !jumpIfMet) {
			OperationExp oc = (OperationExp) condition;
			JumpWriter jump = Jumps.getJumpWriter(oc.getOperation());
			if(jump != null) {
				jump.write(oc, label, this, errors);
				return;
			}
		}
		writer.expWriter.writeExpression(condition, errors);
		writer.instructions.test(Register.RAX);
		writer.instructions.add(jumpIfMet ? OpCode.JNZ : OpCode.JZ, label);
	}
	
	/* ============================================ Conversions =========================================== */
	
	public void writeConversion(VarType from, VarType to, SourceElement source, ErrorWrapper errors) {
		if(!Conversions.checkHasConversionWriter(from, to, source, errors))
			return;
		ConversionWriter cw = Conversions.getConversionWriter(from, to);
		cw.write(from, to, this, errors);
	}
	
}
