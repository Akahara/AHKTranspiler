package fr.wonder.ahk.transpilers.asm_x64.writers;

import fr.wonder.ahk.compiled.expressions.ArrayExp;
import fr.wonder.ahk.compiled.expressions.ConstructorExp;
import fr.wonder.ahk.compiled.expressions.ConversionExp;
import fr.wonder.ahk.compiled.expressions.DirectAccessExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.FunctionCallExp;
import fr.wonder.ahk.compiled.expressions.FunctionExp;
import fr.wonder.ahk.compiled.expressions.FunctionExpression;
import fr.wonder.ahk.compiled.expressions.IndexingExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.NullExp;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.SizeofExp;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.transpilers.common_x64.GlobalLabels;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnreachableException;

public class ExpressionWriter {
	
	private final UnitWriter writer;
	
	public ExpressionWriter(UnitWriter writer) {
		this.writer = writer;
	}
	
	/** writes the given expression to $rax */
	public void writeExpression(Expression exp, ErrorWrapper errors) {
		if(exp instanceof LiteralExp)
			writer.mem.writeTo(Register.RAX, exp, errors);
		else if(exp instanceof FunctionExp)
			writeFunctionExp((FunctionExpression) exp, errors);
		else if(exp instanceof FunctionCallExp)
			writeFunctionExp((FunctionExpression) exp, errors);
		else if(exp instanceof VarExp)
			writer.mem.writeTo(Register.RAX, exp, errors);
		else if(exp instanceof DirectAccessExp)
			writeDirectAccessExp((DirectAccessExp) exp, errors);
		else if(exp instanceof OperationExp)
			writeOperationExp((OperationExp) exp, errors);
		else if(exp instanceof ConversionExp)
			writeConversionExp((ConversionExp) exp, errors);
		else if(exp instanceof ArrayExp)
			writeArrayExp((ArrayExp) exp, errors);
		else if(exp instanceof IndexingExp)
			writeIndexingExp((IndexingExp) exp, errors);
		else if(exp instanceof SizeofExp)
			writeSizeofExp((SizeofExp) exp, errors);
		else if(exp instanceof ConstructorExp)
			writeConstructorExp((ConstructorExp) exp, errors);
		else if(exp instanceof NullExp)
			writeNullExp((NullExp) exp, errors);
		else
			throw new UnreachableException("Unknown expression type " + exp.getClass());
	}

	private void writeFunctionExp(FunctionExpression function,  ErrorWrapper errors) {
		switch(writer.project.manifest.callingConvention) {
		case __stdcall: {
			
			Expression[] arguments = function.getArguments();
			int argsSpace = arguments.length * MemSize.POINTER_SIZE;
			if(argsSpace != 0) {
				writer.instructions.add(OpCode.SUB, Register.RSP, argsSpace);
				writer.mem.addStackOffset(argsSpace);
				for(int i = 0; i < arguments.length; i++) {
					Expression arg = arguments[i];
					writer.mem.writeTo(new MemAddress(Register.RSP, i*MemSize.POINTER_SIZE), arg, errors);
				}
			}
			
			if(function instanceof FunctionExp) {
				writer.instructions.call(writer.getRegistry(((FunctionExp) function).function));
			} else if(function instanceof FunctionCallExp) {
				writeExpression(((FunctionCallExp) function).getFunction(), errors);
				writer.instructions.call(Register.RAX.name);
			} else {
				throw new UnreachableException("Invalid function type " + function.getClass());
			}
			
			writer.mem.addStackOffset(-argsSpace);
			break;
			
		}
		default:
			throw new IllegalStateException("Unimplemented calling convention " + writer.project.manifest.callingConvention);
		}
	}

	private void writeDirectAccessExp(DirectAccessExp exp, ErrorWrapper errors) {
		writeExpression(exp.getStruct(), errors);
		ConcreteType structType = writer.types.getConcreteType((VarStructType) exp.getStruct().getType());
		int offset = structType.getOffset(exp.memberName);
		writer.instructions.mov(Register.RAX, new MemAddress(Register.RAX, offset));
	}
	
	private void writeOperationExp(OperationExp exp, ErrorWrapper errors) {
		if(exp.getOperation() instanceof OverloadedOperatorPrototype) {
			writeFunctionExp(new FunctionExp(exp), errors);
		} else {
			writer.opWriter.writeOperation(exp, errors);
		}
	}
	
	private void writeConversionExp(ConversionExp exp, ErrorWrapper errors) {
		writeExpression(exp.getValue(), errors);
		if(exp.isEffective())
			writer.opWriter.writeConversion(exp.getValue().getType(), exp.castType, errors);
	}
	
	private void writeArrayExp(ArrayExp exp, ErrorWrapper errors) {
		writer.callAlloc(exp.getLength()*MemSize.POINTER_SIZE);
		if(exp.getLength() != 0) {
			writer.mem.addStackOffset(MemSize.POINTER_SIZE);
			writer.instructions.push(Register.RAX);
			for(int i = 0; i < exp.getValues().length; i++) {
				MemAddress address = new MemAddress(Register.RSP).then(i*MemSize.POINTER_SIZE); // [[rsp]+i*elemSize]
				writer.mem.writeTo(address, exp.getValues()[i], errors);
			}
			writer.instructions.pop(Register.RAX);
			writer.mem.addStackOffset(-MemSize.POINTER_SIZE);
		}
	}
	
	private void writeIndexingExp(IndexingExp exp, ErrorWrapper errors) {
		writeExpression(exp.getArray(), errors);
		for(Expression index : exp.getIndices()) {
			MemAddress indexed = writeArrayIndex(index, errors);
			writer.instructions.mov(Register.RAX, indexed);
		}
	}
	
	/**
	 * Writes the index of an indexing expression, check for out of bounds errors
	 * and returns the address of the value at array[index], which can be written to
	 * or read from.
	 * 
	 * <p>
	 * For this to work the array must be stored in rax, otherwise if index is '-1'
	 * (as in {@code array[-1]} to get the last element of <i>array</i>) the
	 * behavior of this method becomes undefined.
	 */
	public MemAddress writeArrayIndex(Expression index, ErrorWrapper errors) {
		if(index instanceof IntLiteral && ((IntLiteral) index).value == -1) {
			String errLabel = writer.getSpecialLabel();
			writer.instructions.mov(Register.RBX, new MemAddress(Register.RAX, -8));
			writer.instructions.test(Register.RBX);
			writer.instructions.add(OpCode.JNZ, errLabel);
			writer.instructions.mov(Register.RAX, -6); // TODO add specific error code
			writer.instructions.call(writer.requireExternLabel(GlobalLabels.SPECIAL_THROW));
			writer.instructions.label(errLabel);
			return new MemAddress(Register.RAX, Register.RBX, 1, -8);
		} else {
			writer.instructions.push(Register.RAX);
			writer.mem.addStackOffset(MemSize.POINTER_SIZE);
			writer.mem.writeTo(Register.RBX, index, errors);
			writer.instructions.pop(Register.RAX);
			writer.mem.addStackOffset(-MemSize.POINTER_SIZE);
			writer.instructions.add(OpCode.SHL, Register.RBX, 3); // scale the index (multiply by 8)
			checkOOB();
			return new MemAddress(Register.RAX, Register.RBX, 1);
		}
	}
	
	/**
	 * Checks for index out of bounds errors.
	 * 
	 * <p>When called, rax must be the array pointer and rbx the <b>scaled</b> index
	 * (multiplied by 8).
	 */
	public void checkOOB() {
		String errLabel = writer.getSpecialLabel();
		String successLabel = writer.getSpecialLabel();
		writer.instructions.test(Register.RBX);
		writer.instructions.add(OpCode.JS, errLabel);
		writer.instructions.cmp(Register.RBX, new MemAddress(Register.RAX, -8));
		writer.instructions.add(OpCode.JL, successLabel);
		writer.instructions.label(errLabel);
		writer.instructions.mov(Register.RAX, -5); // TODO add specific error code (oob)
		writer.instructions.call(writer.requireExternLabel(GlobalLabels.SPECIAL_THROW));
		writer.instructions.label(successLabel);
	}

	private void writeSizeofExp(SizeofExp exp, ErrorWrapper errors) {
		VarType type = exp.getExpression().getType();
		if(type instanceof VarArrayType) {
			writer.mem.writeTo(Register.RBX, exp.getExpression(), errors);
			writer.instructions.clearRegister(Register.RAX); // clear the 32 higher bits
			writer.instructions.mov(Register.RAX, new MemAddress(Register.RBX, -8)); // mov rax,[rbx-8]
			writer.instructions.add(OpCode.SHR, Register.RAX, 3); // shift right by 3 is equivalent to divide by 8 (the pointer size)
		} else {
			errors.add("Sizeof used on non-array type" + exp.getErr());
		}
	}

	private void writeConstructorExp(ConstructorExp exp, ErrorWrapper errors) {
		ConcreteType type = writer.types.getConcreteType(exp.getType());
		ConstructorPrototype constructor = exp.constructor;
		writer.callAlloc(type.size);
		if(constructor.argNames.length == 0)
			return;
		writer.mem.addStackOffset(MemSize.POINTER_SIZE);
		writer.instructions.push(Register.RAX);
		MemAddress instanceAddress = new MemAddress(Register.RSP);
		for(int i = 0; i < constructor.argTypes.length; i++) {
			int fieldOffset = type.getOffset(constructor.argNames[i]);
			MemAddress fieldAddress = new MemAddress(instanceAddress, fieldOffset);
			writer.mem.writeTo(fieldAddress, exp.expressions[i], errors);
		}
		writer.mem.addStackOffset(-MemSize.POINTER_SIZE);
		writer.instructions.pop(Register.RAX);
	}
	
	private void writeNullExp(NullExp exp, ErrorWrapper errors) {
		VarType actualType = exp.getType();
		if(actualType instanceof VarStructType) {
			String nullLabel = writer.getStructNullRegistry(((VarStructType) actualType).structure);
			writer.instructions.mov(Register.RAX, nullLabel);
		} else if(actualType instanceof VarArrayType) {
			writer.instructions.mov(Register.RAX, writer.requireExternLabel(GlobalLabels.GLOBAL_EMPTY_MEM_BLOCK));
		} else if(actualType instanceof VarFunctionType) {
			VarFunctionType funcType = (VarFunctionType) actualType;
			String nullLabel = writer.getFunctionNullRegistry(funcType.returnType, funcType.arguments.length);
			writer.instructions.mov(Register.RAX, nullLabel);
		} else {
			throw new UnreachableException("Unimplemented null: " + actualType);
		}
	}

}
