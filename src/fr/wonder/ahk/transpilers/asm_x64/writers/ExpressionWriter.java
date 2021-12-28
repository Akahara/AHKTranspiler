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
import fr.wonder.ahk.compiled.expressions.ParameterizedExp;
import fr.wonder.ahk.compiled.expressions.SimpleLambdaExp;
import fr.wonder.ahk.compiled.expressions.SizeofExp;
import fr.wonder.ahk.compiled.expressions.UninitializedArrayExp;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarGenericType;
import fr.wonder.ahk.compiled.expressions.types.VarNativeType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.prototypes.BoundOverloadedOperatorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.blueprints.BlueprintTypeParameter;
import fr.wonder.ahk.compiler.types.CompositionOperation;
import fr.wonder.ahk.compiler.types.FunctionOperation;
import fr.wonder.ahk.compiler.types.Operation;
import fr.wonder.ahk.transpilers.asm_x64.units.BlueprintLayout;
import fr.wonder.ahk.transpilers.asm_x64.units.ConcreteType;
import fr.wonder.ahk.transpilers.common_x64.GlobalLabels;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.Address;
import fr.wonder.ahk.transpilers.common_x64.addresses.LabelAddress;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnimplementedException;
import fr.wonder.commons.exceptions.UnreachableException;

public class ExpressionWriter {
	
	private final AbstractWriter writer;
	
	public ExpressionWriter(AbstractWriter writer) {
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
		else if(exp instanceof UninitializedArrayExp)
			writeUninitializedArrayExp((UninitializedArrayExp) exp, errors);
		else if(exp instanceof IndexingExp)
			writeIndexingExp((IndexingExp) exp, errors);
		else if(exp instanceof SizeofExp)
			writeSizeofExp((SizeofExp) exp, errors);
		else if(exp instanceof ConstructorExp)
			writeConstructorExp((ConstructorExp) exp, errors);
		else if(exp instanceof NullExp)
			writeNullExp((NullExp) exp, errors);
		else if(exp instanceof ParameterizedExp)
			writeParameterizedExp((ParameterizedExp) exp, errors);
		else if(exp instanceof SimpleLambdaExp)
			writeSimpleLambdaExp((SimpleLambdaExp) exp, errors);
		else
			throw new UnreachableException("Unknown expression type " + exp.getClass());
	}

	public void writeFunctionExp(FunctionExpression functionExpression, ErrorWrapper errors) {
		
		Expression[] arguments;
		BlueprintTypeParameter[] typesParameters = null;
		int argsSpace;
		
		FunctionPrototype fexpFunctionPrototype = null;	// only used by FunctionExp
		MemAddress fcexpClosurePointer = null;			// only used by FunctionCallExp
		
		// prepare call
		if(functionExpression instanceof FunctionExp) {
			FunctionExp fexp = (FunctionExp) functionExpression;
			arguments = fexp.getArguments();
			fexpFunctionPrototype = fexp.function;
			typesParameters = fexp.typesParameters;
			argsSpace = computeFunctionCallStackSpace(typesParameters, arguments);
		} else if(functionExpression instanceof FunctionCallExp) {
			arguments = ((FunctionCallExp) functionExpression).getArguments();
			argsSpace = computeFunctionCallStackSpace(null, arguments);
			fcexpClosurePointer = new MemAddress(Register.RSP, argsSpace-MemSize.POINTER_SIZE);
		} else {
			throw new IllegalArgumentException("Expression is not callable: " + functionExpression.getClass());
		}
		
		if(argsSpace != 0) {
			writer.mem.addStackOffset(argsSpace);
			writer.instructions.add(OpCode.SUB, Register.RSP, argsSpace);
		}
		
		if(functionExpression instanceof FunctionCallExp) {
			// write and store closure
			writeExpression(((FunctionCallExp) functionExpression).getFunction(), errors);
			writer.instructions.mov(fcexpClosurePointer, Register.RAX);
		}
		
		writeFunctionArguments(typesParameters, arguments, errors);
			
		if(functionExpression instanceof FunctionExp) {
			writer.instructions.call(RegistryManager.getFunctionRegistry(fexpFunctionPrototype));
		} else if(functionExpression instanceof FunctionCallExp) {
			writer.instructions.mov(Register.RAX, fcexpClosurePointer);
			writer.instructions.call(new MemAddress(Register.RAX));
			// when called, the function will have access to the closure in rax
		}
		
		writer.mem.addStackOffset(-argsSpace);
	}
	
	private void writeOperatorFunction(OperationExp exp, ErrorWrapper errors) {
		int argsSpace = computeFunctionCallStackSpace(null, exp.getExpressions());
		writer.mem.addStackOffset(argsSpace);
		writer.instructions.add(OpCode.SUB, Register.RSP, argsSpace);
		writeFunctionArguments(null, exp.getOperands(), errors);
		OverloadedOperatorPrototype op = (OverloadedOperatorPrototype) exp.getOperation();
		if(op instanceof BoundOverloadedOperatorPrototype) {
			BoundOverloadedOperatorPrototype bop = (BoundOverloadedOperatorPrototype) op;
			writer.mem.moveData(Register.RAX, writer.sectionArguments.getGIPLocation(bop.genericType, bop.usedBlueprint));
			int offsetInGIP = BlueprintLayout.getOperatorOffset(bop.usedBlueprint, bop.originalOperator);
			writer.instructions.call(new MemAddress(Register.RAX, offsetInGIP*MemSize.POINTER_SIZE));
		} else {
			writer.instructions.call(RegistryManager.getFunctionRegistry(op.function));
		}
		writer.mem.addStackOffset(-argsSpace);
	}
	
	private int computeFunctionCallStackSpace(BlueprintTypeParameter[] typesParameters, Expression[] args) {
		int argsSpace = args.length * MemSize.POINTER_SIZE;
		if(typesParameters != null)
			argsSpace += typesParameters.length * MemSize.POINTER_SIZE;
		return argsSpace;
	}
	
	private void writeFunctionArguments(
			BlueprintTypeParameter[] typesParameters,
			Expression[] args,
			ErrorWrapper errors) {
		
		for(int i = 0; i < args.length; i++) {
			Expression arg = args[i];
			Address argAddress = new MemAddress(Register.RSP, i*MemSize.POINTER_SIZE);
			writer.mem.writeTo(argAddress, arg, errors);
		}
		
		if(typesParameters != null) {
			for(int i = 0; i < typesParameters.length; i++) {
				String typeImplReg = RegistryManager.getStructBlueprintImplRegistry(typesParameters[i].implementation);
				writer.instructions.mov(new MemAddress(Register.RSP, (args.length+i) * MemSize.POINTER_SIZE), typeImplReg);
				// TODO make some form of global layout for arguments
				// currently both MemoryManager~#Scope and this method rely on a
				// particular layout that must be maintained in both files
			}
		}
	}

	private void writeDirectAccessExp(DirectAccessExp exp, ErrorWrapper errors) {
		writeExpression(exp.getStruct(), errors);
		ConcreteType structType = writer.unitWriter.types.getConcreteType(((VarStructType) exp.getStruct().getType()).structure);
		int offset = structType.getOffset(exp.memberName);
		writer.instructions.mov(Register.RAX, new MemAddress(Register.RAX, offset));
	}
	
	private void writeOperationExp(OperationExp exp, ErrorWrapper errors) {
		Operation operation = exp.getOperation();
		if(operation instanceof OverloadedOperatorPrototype) {
			writeOperatorFunction(exp, errors);
		} else if(operation instanceof FunctionOperation) {
			writer.closureWriter.writeFuncOperation(exp, errors);
		} else if(operation instanceof CompositionOperation) {
			writer.closureWriter.writeCompositionOperation(exp, errors);
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
		writer.unitWriter.callAlloc(exp.getLength() * MemSize.POINTER_SIZE);
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
	
	private void writeUninitializedArrayExp(UninitializedArrayExp exp, ErrorWrapper errors) {
		writeExpression(exp.getDefaultComponentValue(), errors);
		writer.instructions.push(Register.RAX);
		writeExpression(exp.getSize(), errors);
		writer.instructions.push(Register.RAX);
		writer.instructions.add(OpCode.SHL, Register.RAX, 3); // multiply by 8 (element count to byte count)
		writer.unitWriter.callAlloc(Register.RAX);
		writer.instructions.mov(Register.RDX, Register.RAX);
		writer.instructions.mov(Register.RDI, Register.RAX);
		writer.instructions.pop(Register.RCX);
		writer.instructions.pop(Register.RAX);
		writer.instructions.add(OpCode.CLD);
		writer.instructions.repeat(OpCode.STOSQ);
		writer.instructions.mov(Register.RAX, Register.RDX);
	}
	
	private void writeIndexingExp(IndexingExp exp, ErrorWrapper errors) {
		writeExpression(exp.getArray(), errors);
		VarType componentType = exp.getArray().getType();
		for(Expression index : exp.getIndices()) {
			componentType = ((VarArrayType) componentType).componentType;
			String oobLabel = writer.unitWriter.getSpecialLabel();
			String endLabel = writer.unitWriter.getSpecialLabel();
			if(index instanceof IntLiteral && ((IntLiteral) index).value == -1) {
				writer.instructions.mov(Register.RBX, new MemAddress(Register.RAX, -8));
				writer.instructions.test(Register.RBX);
				writer.instructions.add(OpCode.JZ, oobLabel);
				writer.instructions.mov(Register.RAX, new MemAddress(Register.RAX, Register.RBX, 1, -8));
			} else {
				writer.instructions.push(Register.RAX);
				writer.mem.addStackOffset(MemSize.POINTER_SIZE);
				writer.mem.writeTo(Register.RBX, index, errors);
				writer.instructions.pop(Register.RAX);
				writer.mem.addStackOffset(-MemSize.POINTER_SIZE);
				writer.instructions.add(OpCode.SHL, Register.RBX, 3); // scale the index (multiply by 8)
				writer.instructions.test(Register.RBX);
				writer.instructions.add(OpCode.JS, oobLabel);
				writer.instructions.cmp(Register.RBX, new MemAddress(Register.RAX, -8));
				writer.instructions.add(OpCode.JGE, oobLabel);
				writer.instructions.mov(Register.RAX, new MemAddress(Register.RAX, Register.RBX, 1));
			}
			writer.instructions.jmp(endLabel);
			writer.instructions.label(oobLabel);
			writeDefaultValue(componentType, index, errors); // on oob, write a default value instead of throwing an error
			writer.instructions.label(endLabel);
		}
	}
	
	private void writeSizeofExp(SizeofExp exp, ErrorWrapper errors) {
		VarType type = exp.getExpression().getType();
		if(type instanceof VarArrayType) {
			writer.mem.writeTo(Register.RBX, exp.getExpression(), errors);
			writer.instructions.mov(Register.RAX, new MemAddress(Register.RBX, -8));
			writer.instructions.add(OpCode.SHR, Register.RAX, 3); // shift right by 3 is equivalent to divide by 8 (the pointer size)
		} else {
			errors.add("Sizeof used on non-array type" + exp.getErr());
		}
	}

	private void writeConstructorExp(ConstructorExp exp, ErrorWrapper errors) {
		ConcreteType type = writer.unitWriter.types.getConcreteType(exp.getType().structure);
		ConstructorPrototype constructor = exp.constructor;
		writer.unitWriter.callAlloc(type.size);
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
	
	private void writeDefaultValue(VarType type, SourceElement sourceElement, ErrorWrapper errors) {
		if(type instanceof VarStructType) {
			String nullLabel = writer.unitWriter.registries.getStructNullRegistry(((VarStructType) type).structure);
			writer.instructions.mov(Register.RAX, nullLabel);
		} else if(type instanceof VarArrayType) {
			writer.instructions.mov(Register.RAX, writer.unitWriter.requireExternLabel(GlobalLabels.GLOBAL_EMPTY_MEM_BLOCK));
		} else if(type instanceof VarFunctionType) {
			VarFunctionType funcType = (VarFunctionType) type;
			writer.closureWriter.writeConstantClosure(funcType.returnType, funcType.arguments.length);
		} else if(type instanceof VarGenericType) {
			throw new UnimplementedException("Generic type null instance" + sourceElement.getErr());
		} else if(type == VarType.STR) {
			writer.instructions.mov(Register.RAX, writer.unitWriter.requireExternLabel(GlobalLabels.GLOBAL_EMPTY_MEM_BLOCK));
		} else if(type instanceof VarNativeType) {
			writer.instructions.clearRegister(Register.RAX);
		} else {
			throw new UnreachableException("Unimplemented null: " + type);
		}
	}
	
	private void writeNullExp(NullExp exp, ErrorWrapper errors) {
		VarType type = exp.getType().getActualType();
		writeDefaultValue(type, exp, errors);
	}

	private void writeParameterizedExp(ParameterizedExp exp, ErrorWrapper errors) {
		throw new UnimplementedException("Parametrized expressions"); // FIX implement asm parameterized expressions
	}
	
	private void writeSimpleLambdaExp(SimpleLambdaExp exp, ErrorWrapper errors) {
		if(exp.lambda.hasClosureArguments())
			throw new UnimplementedException("lambda with closure arguments"); // TODO implement lambda closure arguments
		String lambdaLabel = writer.unitWriter.registries.getLambdaClosureRegistry(exp.lambda);
		writer.instructions.mov(Register.RAX, new LabelAddress(lambdaLabel));
	}

}
