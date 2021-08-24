package fr.wonder.ahk.transpilers.asm_x64.writers;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.compiler.types.CompositionOperation;
import fr.wonder.ahk.compiler.types.FunctionOperation;
import fr.wonder.ahk.compiler.types.NativeOperation;
import fr.wonder.ahk.compiler.types.Operation;
import fr.wonder.ahk.transpilers.common_x64.GlobalLabels;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnreachableException;

public class AsmClosuresWriter {

	private final UnitWriter writer;
	
	public AsmClosuresWriter(UnitWriter writer) {
		this.writer = writer;
	}
	
	public void writeFuncOperation(OperationExp exp, ErrorWrapper errors) {
		FunctionOperation op = (FunctionOperation) exp.getOperation();
		
		writer.instructions.add(OpCode.SUB, Register.RSP, 16);
		writer.mem.addStackOffset(16);
		writer.mem.writeTo(new MemAddress(Register.RSP, 0), exp.getLeftOperand(), errors);
		writer.mem.writeTo(new MemAddress(Register.RSP, 8), exp.getRightOperand(), errors);
		writer.mem.addStackOffset(-16);
		writer.callAlloc(5*8);
		
		MemAddress closureAddress = new MemAddress(Register.RAX);
		String operationClosureRegistry = getOperationClosureRegistry(op.resultOperation);
		int argumentsCount = ((VarFunctionType) op.resultType).arguments.length;
		
		writer.instructions.mov(closureAddress, writer.requireExternLabel(GlobalLabels.CLOSURE_RUN_OPERATION_FUNC));
		writer.instructions.mov(closureAddress.addOffset(8), argumentsCount);
		writer.instructions.pop(Register.RBX); // first function
		writer.instructions.mov(closureAddress.addOffset(16), Register.RBX);
		writer.instructions.pop(Register.RBX); // second function
		writer.instructions.mov(closureAddress.addOffset(24), Register.RBX);
		writer.instructions.mov(closureAddress.addOffset(32), operationClosureRegistry);
	}
	
	private String getOperationClosureRegistry(Operation op) {
		if(op instanceof NativeOperation) {
			return RegistryManager.getOperationClosureRegistry((NativeOperation) op);
		} else if(op instanceof OverloadedOperatorPrototype) {
			String registry = RegistryManager.getClosureRegistry(((OverloadedOperatorPrototype) op).function);
			return writer.requireExternLabel(registry);
		} else {
			throw new UnreachableException("Invalid operation type " + op.getClass());
		}
	}

	public void writeCompositionOperation(OperationExp exp, ErrorWrapper errors) {
		CompositionOperation op = (CompositionOperation) exp.getOperation();
		
		writer.instructions.add(OpCode.SUB, Register.RSP, 16);
		writer.mem.addStackOffset(16);
		Expression firstFunctionExp = op.isLeftFirstApplied() ? exp.getLeftOperand() : exp.getRightOperand();
		Expression secondFunctionExp = op.isLeftFirstApplied() ? exp.getRightOperand() : exp.getLeftOperand();
		writer.mem.writeTo(new MemAddress(Register.RSP, 0), firstFunctionExp, errors);
		writer.mem.writeTo(new MemAddress(Register.RSP, 8), secondFunctionExp, errors);
		writer.mem.addStackOffset(-16);
		
		writer.callAlloc(4*8);
		MemAddress closureAddress = new MemAddress(Register.RAX);
		writer.instructions.mov(closureAddress, writer.requireExternLabel(GlobalLabels.CLOSURE_RUN_COMPOSED_FUNC));
		writer.instructions.mov(closureAddress.addOffset(8), op.getFirstApplied().arguments.length);
		writer.instructions.pop(Register.RBX); // first function
		writer.instructions.mov(closureAddress.addOffset(16), Register.RBX);
		writer.instructions.pop(Register.RBX); // second function
		writer.instructions.mov(closureAddress.addOffset(24), Register.RBX);
	}
	
	public void writeConstantClosure(VarType returnType, int argsCount) {
		String constantValue;
		if(returnType instanceof VarStructType)
			constantValue = writer.registries.getStructNullRegistry(((VarStructType) returnType).structure);
		else
			constantValue = "0";
		
		writer.callAlloc(3*8);
		MemAddress closureAddress = new MemAddress(Register.RAX);
		writer.instructions.mov(closureAddress, writer.requireExternLabel(GlobalLabels.CLOSURE_RUN_CONSTANT_FUNC));
		writer.instructions.mov(closureAddress.addOffset(8), argsCount);
		writer.instructions.mov(closureAddress.addOffset(16), constantValue);
	}
	
}
