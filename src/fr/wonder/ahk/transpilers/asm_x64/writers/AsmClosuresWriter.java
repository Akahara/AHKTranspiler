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
import fr.wonder.ahk.transpilers.common_x64.addresses.Address;
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
		if(exp.getOperation().hasLeftOperand())
			writeFuncOperation2Operands(exp, errors);
		else
			writeFuncOperation1Operand(exp, errors);
	}

	private void writeFuncOperation2Operands(OperationExp exp, ErrorWrapper errors) {
		FunctionOperation op = (FunctionOperation) exp.getOperation();
		
		int argumentsCount = ((VarFunctionType) op.resultType).arguments.length;
		
		writer.instructions.add(OpCode.SUB, Register.RSP, 16);
		writer.mem.addStackOffset(16);
		MemAddress firstOperandLoc = new MemAddress(Register.RSP, 0);
		MemAddress secondOperandLoc = new MemAddress(Register.RSP, 8);
		writer.mem.writeTo(firstOperandLoc, exp.getLeftOperand(), errors);
		writer.mem.writeTo(secondOperandLoc, exp.getRightOperand(), errors);
		if(!(exp.getLOType() instanceof VarFunctionType)) {
			Address constantAddressInClosure = writeConstantClosure(argumentsCount);
			writer.instructions.mov(Register.RBX, firstOperandLoc);
			writer.instructions.mov(constantAddressInClosure, Register.RBX);
			writer.instructions.mov(firstOperandLoc, Register.RAX);
		}
		if(!(exp.getROType() instanceof VarFunctionType)) {
			Address constantAddressInClosure = writeConstantClosure(argumentsCount);
			writer.instructions.mov(Register.RBX, secondOperandLoc);
			writer.instructions.mov(constantAddressInClosure, Register.RBX);
			writer.instructions.mov(secondOperandLoc, Register.RAX);
		}
		writer.mem.addStackOffset(-16);
		
		MemAddress closureAddress = new MemAddress(Register.RAX);
		String operationRegistry = getOperationRegistry(op.resultOperation);
		
		writer.callAlloc(5*8);
		writer.instructions.mov(closureAddress, writer.requireExternLabel(GlobalLabels.CLOSURE_RUN_OPERATION_2));
		writer.instructions.mov(closureAddress.addOffset(8), argumentsCount);
		writer.instructions.pop(Register.RBX); // first function
		writer.instructions.mov(closureAddress.addOffset(16), Register.RBX);
		writer.instructions.pop(Register.RBX); // second function
		writer.instructions.mov(closureAddress.addOffset(24), Register.RBX);
		writer.instructions.mov(closureAddress.addOffset(32), operationRegistry);
	}
	
	private void writeFuncOperation1Operand(OperationExp exp, ErrorWrapper errors) {
		FunctionOperation op = (FunctionOperation) exp.getOperation();
		
		int argumentsCount = ((VarFunctionType) op.resultType).arguments.length;
		
		writer.instructions.add(OpCode.SUB, Register.RSP, 8);
		writer.mem.addStackOffset(8);
		MemAddress operandLoc = new MemAddress(Register.RSP, 0);
		writer.mem.writeTo(operandLoc, exp.getRightOperand(), errors);
		if(!(exp.getROType() instanceof VarFunctionType)) {
			Address constantAddressInClosure = writeConstantClosure(argumentsCount);
			writer.instructions.mov(Register.RBX, operandLoc);
			writer.instructions.mov(constantAddressInClosure, Register.RBX);
			writer.instructions.mov(operandLoc, Register.RAX);
		}
		writer.mem.addStackOffset(-8);
		
		MemAddress closureAddress = new MemAddress(Register.RAX);
		String operationRegistry = getOperationRegistry(op.resultOperation);
		
		writer.callAlloc(4*8);
		writer.instructions.mov(closureAddress, writer.requireExternLabel(GlobalLabels.CLOSURE_RUN_OPERATION_1));
		writer.instructions.mov(closureAddress.addOffset(8), argumentsCount);
		writer.instructions.pop(Register.RBX); // function
		writer.instructions.mov(closureAddress.addOffset(16), Register.RBX);
		writer.instructions.mov(closureAddress.addOffset(24), operationRegistry);
	}
	
	private String getOperationRegistry(Operation op) {
		String registry;
		if(op instanceof NativeOperation) {
			registry = RegistryManager.getOperationClosureRegistry((NativeOperation) op);
		} else if(op instanceof OverloadedOperatorPrototype) {
			registry = RegistryManager.getFunctionRegistry(((OverloadedOperatorPrototype) op).function);
		} else {
			throw new UnreachableException("Invalid operation type " + op.getClass());
		}
		return writer.requireExternLabel(registry);
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
		writer.instructions.mov(closureAddress, writer.requireExternLabel(GlobalLabels.CLOSURE_RUN_COMPOSED));
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
		Address constantAddressInClosure = writeConstantClosure(argsCount);
		writer.instructions.mov(constantAddressInClosure, constantValue);
	}
	
	/**
	 * Creates a constant closure (see closures.fasm) and returns
	 * the address in which to put the constant.
	 */
	private MemAddress writeConstantClosure(int argsCount) {
		writer.callAlloc(3*8);
		MemAddress closureAddress = new MemAddress(Register.RAX);
		writer.instructions.mov(closureAddress, writer.requireExternLabel(GlobalLabels.CLOSURE_RUN_CONSTANT));
		writer.instructions.mov(closureAddress.addOffset(8), argsCount);
		return closureAddress.addOffset(16);
	}
	
}
