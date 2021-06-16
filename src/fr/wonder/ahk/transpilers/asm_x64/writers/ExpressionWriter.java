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
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.transpilers.common_x64.GlobalLabels;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnimplementedException;

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
			writeFunctionExp((FunctionExp) exp, errors);
		else if(exp instanceof VarExp)
			writer.mem.writeTo(Register.RAX, exp, errors);
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
	
	private void writeFunctionExp(FunctionPrototype function, Expression[] arguments,  ErrorWrapper errors) {
		switch(writer.project.manifest.callingConvention) {
		case __stdcall: {
			int argsSpace = FunctionWriter.getArgumentsSize(function);
			if(argsSpace != 0) {
				writer.instructions.add(OpCode.SUB, Register.RSP, argsSpace);
				writer.mem.addStackOffset(argsSpace);
			}
			int offset = 0;
			for(int i = 0; i < arguments.length; i++) {
				Expression arg = arguments[i];
				writer.mem.writeTo(new MemAddress(Register.RSP, offset), arg, errors);
				offset += MemSize.getPointerSize(arg.getType()).bytes;
			}
			writer.instructions.call(UnitWriter.getRegistry(function));
			writer.mem.addStackOffset(-argsSpace);
			break;
		}
		default:
			throw new IllegalStateException("Unimplemented calling convention " + writer.project.manifest.callingConvention);
		}
	}
	
	private void writeOperationExp(OperationExp exp, ErrorWrapper errors) {
		if(exp.getOperation() instanceof FunctionSection) { // FIX when operator overloading is implemented...
			// functionSections do not implement operation, use the appropriate class instead
			throw new UnimplementedException();
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
		int elemSize = MemSize.getPointerSize(exp.getType().componentType).bytes;
		writer.callAlloc(exp.getLength()*elemSize);
		if(exp.getLength() != 0) {
			writer.mem.addStackOffset(MemSize.POINTER_SIZE);
			writer.instructions.push(Register.RAX);
			for(int i = 0; i < exp.getValues().length; i++) {
				MemAddress address = new MemAddress(Register.RSP).then(i*elemSize); // [[rsp]+i*elemSize]
				writer.mem.writeTo(address, exp.getValues()[i], errors);
			}
			writer.instructions.pop(Register.RAX);
			writer.mem.addStackOffset(-MemSize.POINTER_SIZE);
		}
	}
	
	private void writeIndexingExp(IndexingExp exp, ErrorWrapper errors) {
		writer.mem.writeTo(Register.RAX, exp.getArray(), errors);
		VarArrayType arrayType = (VarArrayType) exp.getArray().getType();
		for(Expression index : exp.getIndices()) {
			String specialLabel1 = writer.getSpecialLabel();
			if(index instanceof IntLiteral && ((IntLiteral) index).value == -1) {
				writer.instructions.cmp(new MemAddress(Register.RAX, -4), 0);
				writer.instructions.jmp(specialLabel1);
				writer.instructions.mov(Register.RAX, -6); // TODO add specific error code
				writer.instructions.call(GlobalLabels.SPECIAL_THROW);
				writer.instructions.label(specialLabel1);
				writer.instructions.add(OpCode.ADD, Register.RAX, new MemAddress(Register.RAX, -8));
				writer.instructions.add(OpCode.SUB, Register.RAX, MemSize.getPointerSize(arrayType.componentType));
				writer.instructions.mov(Register.RAX, new MemAddress(Register.RAX)); // mov rax,[rax]
				throw new UnimplementedException("array[-1] indexing won't work untill the memory system is not changed, see the FIX below");
			} else {
				String specialLabel2 = writer.getSpecialLabel();
				writer.instructions.push(Register.RAX);
				writer.mem.addStackOffset(MemSize.POINTER_SIZE);
				writer.mem.writeTo(Register.RAX, index, errors);
				writer.instructions.pop(Register.RBX); // rbx is the array pointer
				writer.mem.addStackOffset(-MemSize.POINTER_SIZE);
				int csize = MemSize.getPointerSize(arrayType.componentType).bytes;
				if(csize == 8)
					writer.instructions.add(OpCode.SHL, Register.RAX, 3);
				else
					writer.instructions.add(OpCode.IMUL, Register.RAX, csize);
				// TODO0 check if the scaled index overflows the 64 bits limit
				writer.instructions.test(Register.RAX);
				writer.instructions.add(OpCode.JS, specialLabel2); // the index 
				writer.instructions.cmp(new MemAddress(Register.RBX, -4), Register.EAX); // FIX CHANGE THE MEM SYSTEM
																						// so that the size of a memory block is stored in 8 bits rather than 4
				writer.instructions.add(OpCode.JLE, specialLabel2);
				writer.instructions.jmp(specialLabel1);
				writer.instructions.label(specialLabel2);
				writer.instructions.mov(Register.RAX, -5); // TODO add specific error code
				writer.instructions.call(GlobalLabels.SPECIAL_THROW);
				writer.instructions.label(specialLabel1);
				writer.instructions.mov(Register.RAX, new MemAddress(Register.RAX, Register.RBX, 1)); // mov rax,[rax+rbx]
			}
		}
	}

	private void writeSizeofExp(SizeofExp exp, ErrorWrapper errors) {
		VarType type = exp.getExpression().getType();
		if(type instanceof VarArrayType) {
			writer.mem.writeTo(Register.RBX, exp.getExpression(), errors);
			writer.instructions.clearRegister(Register.RAX); // clear the 32 higher bits
			writer.instructions.mov(Register.EAX, new MemAddress(Register.RBX, -4)); // mov eax,[rbx-4]
			int csize = MemSize.getPointerSize(((VarArrayType) type).componentType).bytes;
			if(csize == 8)
				writer.instructions.add(OpCode.SHR, Register.RAX, 3);
			else {
				writer.instructions.clearRegister(Register.RDX);
				writer.instructions.mov(Register.RBX, csize);
				writer.instructions.add(OpCode.DIV, Register.RBX);
			}
		} else {
			errors.add("Sizeof used on non-array type" + exp.getErr());
		}
	}

}
