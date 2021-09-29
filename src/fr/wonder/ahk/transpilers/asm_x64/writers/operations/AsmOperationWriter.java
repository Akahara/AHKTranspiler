package fr.wonder.ahk.transpilers.asm_x64.writers.operations;

import static fr.wonder.ahk.compiled.expressions.Operator.EQUALS;
import static fr.wonder.ahk.compiled.expressions.Operator.LOWER;
import static fr.wonder.ahk.compiled.expressions.Operator.NEQUALS;
import static fr.wonder.ahk.compiled.expressions.types.VarType.INT;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.types.NativeOperation;
import fr.wonder.ahk.compiler.types.Operation;
import fr.wonder.ahk.transpilers.asm_x64.writers.FunctionWriter;
import fr.wonder.ahk.transpilers.asm_x64.writers.RegistryManager;
import fr.wonder.ahk.transpilers.asm_x64.writers.operations.Operations.NativeFunctionWriter;
import fr.wonder.ahk.transpilers.asm_x64.writers.operations.Operations.OperationWriter;
import fr.wonder.ahk.transpilers.common_x64.GlobalLabels;
import fr.wonder.ahk.transpilers.common_x64.InstructionSet;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.ImmediateValue;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.ahk.transpilers.common_x64.instructions.OperationParameter;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.types.Tuple;
import fr.wonder.commons.utils.Assertions;

public class AsmOperationWriter {
	
	private static final Map<Operation, JumpWriter> conditionalJumps = new HashMap<>();
	private static final Map<Tuple<VarType, VarType>, ConversionWriter> conversions = new HashMap<>();
	
	private static interface JumpWriter {
		
		void write(OperationExp condition, String label, AsmOperationWriter asmWriter, ErrorWrapper errors);
		
	}
	
	private static interface ConversionWriter {
		
		void write(VarType from, VarType to, AsmOperationWriter asmWriter, ErrorWrapper errors);
		
	}
	
	static {
		conditionalJumps.put(NativeOperation.getOperation(INT, INT, EQUALS, false), Jumps::jump_intEQUint);
		conditionalJumps.put(NativeOperation.getOperation(INT, INT, NEQUALS, false), Jumps::jump_intNEQUint);
		conditionalJumps.put(NativeOperation.getOperation(INT, INT, LOWER, false), Jumps::jump_intLTint);
		
		Assertions.assertNull(conditionalJumps.get(null), "An unimplemented native operation was given an asm jump implementation");
	}
	
	static {
		conversions.put(new Tuple<>(VarType.INT, VarType.FLOAT), Conversions::conv_intTOfloat);
		conversions.put(new Tuple<>(VarType.FLOAT, VarType.INT), Conversions::conv_floatTOint);
		conversions.put(new Tuple<>(VarType.BOOL, VarType.INT), (from, to, writer, errors) -> {}); // NOOP
	}
	
	final FunctionWriter writer;
	
	public AsmOperationWriter(FunctionWriter writer) {
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
	
	public static boolean operationCanBeClosure(NativeOperation op) {
		return Operations.nativeFunctions.containsKey(op);
	}
	
	/* ============================================ Operations ============================================ */
	
	public void writeOperation(OperationExp exp, ErrorWrapper errors) {
		OperationWriter opw = Operations.nativeOperations.get(exp.getOperation());
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
	
	void forcePrepareRAXRBX(Expression e1, Expression e2, ErrorWrapper errors) {
		writer.mem.writeTo(Register.RAX, e1, errors);
		writer.mem.addStackOffset(8);
		writer.instructions.push(Register.RAX);
		writer.mem.writeTo(Register.RBX, e2, errors);
		writer.mem.addStackOffset(-8);
		writer.instructions.pop(Register.RAX);
	}
	
	void simpleFPUOperation(Expression e1, Expression e2, boolean commutativeOperation, OpCode opCode, ErrorWrapper errors) {
		OperationParameter ro = prepareRAXRBX(e1, e2, commutativeOperation, errors);
		MemAddress floatst = writer.unitWriter.requireExternLabel(GlobalLabels.ADDRESS_FLOATST);
		writer.instructions.mov(floatst, Register.RAX);
		writer.instructions.addCasted(OpCode.FLD, MemSize.QWORD, floatst);
		writer.mem.moveData(floatst, ro);
		writer.instructions.addCasted(OpCode.FLD, MemSize.QWORD, floatst);
		writer.instructions.add(opCode);
		writer.instructions.addCasted(OpCode.FSTP, MemSize.QWORD, floatst);
		writer.instructions.mov(Register.RAX, floatst);
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
	
	/* ============================================ Conversions =========================================== */
	
	public void writeConversion(VarType from, VarType to, ErrorWrapper errors) {
		ConversionWriter cw = conversions.get(new Tuple<>(from, to));
		if(cw == null)
			throw new IllegalArgumentException("Unimplemented conversion " + from + " -> " + to);
		cw.write(from, to, this, errors);
	}
	
}
