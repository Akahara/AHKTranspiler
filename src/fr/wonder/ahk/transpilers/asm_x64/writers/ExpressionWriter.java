package fr.wonder.ahk.transpilers.asm_x64.writers;

import fr.wonder.ahk.compiled.expressions.ArrayExp;
import fr.wonder.ahk.compiled.expressions.ConversionExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.FunctionExp;
import fr.wonder.ahk.compiled.expressions.IndexingExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.SizeofExp;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarNativeType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.transpilers.asm_x64.writers.memory.ComplexLoc;
import fr.wonder.ahk.transpilers.asm_x64.writers.memory.DirectLoc;
import fr.wonder.ahk.transpilers.asm_x64.writers.memory.MemoryLoc;
import fr.wonder.ahk.transpilers.asm_x64.writers.memory.MemoryManager;
import fr.wonder.ahk.transpilers.asm_x64.writers.memory.VarLocation;
import fr.wonder.ahk.utils.ErrorWrapper;

public class ExpressionWriter {
	
	private final UnitWriter writer;
	
	public ExpressionWriter(UnitWriter writer) {
		this.writer = writer;
	}
	
	/** writes the given expression to $rax */
	public void writeExpression(Expression exp, ErrorWrapper errors) {
		if(exp instanceof NoneExp)
			writer.buffer.writeLine("mov rax,0");
		else if(exp instanceof LiteralExp)
			writer.mem.writeTo(DirectLoc.LOC_RAX, exp, errors);
		else if(exp instanceof FunctionExp)
			writeFunctionExp((FunctionExp) exp, errors);
		else if(exp instanceof VarExp)
			writer.mem.writeTo(DirectLoc.LOC_RAX, exp, errors);
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
		else
			throw new IllegalStateException("Unknown expression type " + exp.getClass());
	}

	private void writeFunctionExp(FunctionExp exp, ErrorWrapper errors) {
		writeFunctionExp(exp.function, exp.getArguments(), errors);
	}
	
	private void writeFunctionExp(FunctionSection function, Expression[] arguments,  ErrorWrapper errors) {
		switch(writer.projectHandle.manifest.callingConvention) {
		case __stdcall: {
			int argsSpace = FunctionWriter.getArgumentsSize(function);
			if(argsSpace != 0)
				writer.buffer.writeLine("sub rsp," + argsSpace);
			writer.mem.addStackOffset(argsSpace);
			int offset = 0;
			for(int i = 0; i < arguments.length; i++) {
				Expression arg = arguments[i];
				writer.mem.writeTo(new MemoryLoc(VarLocation.REG_RSP, offset), arg, errors);
				offset += arg.getType().getSize();
			}
			writer.buffer.writeLine("call " + writer.getRegistry(function));
			writer.mem.restoreStackOffset();
			break;
		}
		default:
			throw new IllegalStateException("Unimplemented calling convention " + writer.projectHandle.manifest.callingConvention);
		}
	}
	
	private void writeOperationExp(OperationExp exp, ErrorWrapper errors) {
		if(exp.operation instanceof FunctionSection) {
			FunctionSection func = (FunctionSection) exp.operation;
			Expression leftOperand = exp.getLeftOperand() == null ? new NoneExp(func.argumentTypes[0].getSize()) : exp.getLeftOperand();
			writeFunctionExp(func, new Expression[] { leftOperand, exp.getRightOperand() }, errors);
		} else {
			boolean nativeExists = writer.asmWriter.writeOperation(exp.operation, exp.getLeftOperand(), exp.getRightOperand(), errors);
			if(!nativeExists)
				errors.add("Unimplemented assembly operation! " + exp.operationString() + exp.getErr());
		}
	}
	
	private void writeConversionExp(ConversionExp exp, ErrorWrapper errors) {
		writeExpression(exp.getValue(), errors);
		if(exp.isEffective())
			writer.asmWriter.writeConversion(exp.getValue().getType(), exp.castType, errors);
	}
	
	private void writeArrayExp(ArrayExp exp, ErrorWrapper errors) {
		int elemSize;
		if(exp.getType() instanceof VarNativeType) // FIX do not use the array type but rather the component type
			elemSize = exp.getType().getSize();
		else
			elemSize = MemoryManager.POINTER_SIZE;
		writer.callAlloc(exp.getLength()*elemSize);
		if(exp.getLength() != 0) {
			writer.mem.addStackOffset(8);
			writer.buffer.writeLine("push rax");
			for(int i = 0; i < exp.getValues().length; i++)
				writer.mem.writeTo(new ComplexLoc(VarLocation.REG_RSP, 0, i*elemSize), exp.getValues()[i], errors);
			writer.buffer.writeLine("pop rax");
			writer.mem.restoreStackOffset();
		}
	}
	
	private void writeIndexingExp(IndexingExp exp, ErrorWrapper errors) {
		writer.mem.writeTo(DirectLoc.LOC_RAX, exp.getArray(), errors);
		VarArrayType arrayType = (VarArrayType) exp.getArray().getType();
		for(Expression index : exp.getIndices()) {
			String specialLabel = writer.getSpecialLabel();
			if(index instanceof IntLiteral && ((IntLiteral) index).value == -1) {
				
			} else {
				writer.buffer.writeLine("push rax");
				writer.mem.addStackOffset(8);
				writer.mem.writeTo(DirectLoc.LOC_RAX, index, errors);
				writer.buffer.writeLine("pop rbx"); // rbx is the array pointer
				writer.mem.restoreStackOffset();
				// TODO0 check if imul/shl exceeds the 64 bits bounds (check the ALU flags)
				int csize = getComponentSize(arrayType);
				if(csize == 8)
					writer.buffer.writeLine("shl rax,3");
				else
					writer.buffer.writeLine("imul rax,"+csize);
				writer.buffer.writeLine("test rax,rax");
				writer.buffer.writeLine("jns "+specialLabel); // the index 
				writer.buffer.writeLine("cmp dword[rbx-4],eax");
				writer.buffer.writeLine("jl "+specialLabel);
				writer.buffer.writeLine("mov rax,-5"); // FIX add specific error code
				writer.buffer.writeLine("call "+UnitWriter.SPECIAL_THROW);
				writer.buffer.appendLine(specialLabel+":");
				writer.mem.moveData(new MemoryLoc("rax", "rbx"), DirectLoc.LOC_RAX);
			}
		}
	}

	private void writeSizeofExp(SizeofExp exp, ErrorWrapper errors) {
		VarType type = exp.getExpression().getType();
		if(type instanceof VarArrayType) {
			writer.mem.writeTo(DirectLoc.LOC_RBX, exp.getExpression(), errors);
			writer.buffer.writeLine("xor rax,rax"); // clear the 32 higher bits
			writer.buffer.writeLine("mov eax,[rbx-4]");
			int csize = getComponentSize((VarArrayType) type);
			if(csize == 8)
				writer.buffer.writeLine("shr rax,3");
			else {
				writer.buffer.writeLine("xor rdx,rdx");
				writer.buffer.writeLine("mov rbx,"+csize);
				writer.buffer.writeLine("div rbx");
			}
		} else {
			errors.add("Sizeof used on non-array type" + exp.getErr());
		}
	}

	public static int getComponentSize(VarArrayType arrayType) {
		if(arrayType.componentType instanceof VarNativeType)
			return arrayType.componentType.getSize();
		else
			return MemoryManager.POINTER_SIZE;
	}
	
}
