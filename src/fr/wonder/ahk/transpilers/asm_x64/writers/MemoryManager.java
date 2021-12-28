package fr.wonder.ahk.transpilers.asm_x64.writers;

import fr.wonder.ahk.compiled.expressions.DirectAccessExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.IndexingExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.transpilers.asm_x64.units.ConcreteType;
import fr.wonder.ahk.transpilers.asm_x64.units.DummyVariableDeclaration;
import fr.wonder.ahk.transpilers.asm_x64.units.FunctionArgumentsLayout;
import fr.wonder.ahk.transpilers.asm_x64.units.NoneExp;
import fr.wonder.ahk.transpilers.common_x64.GlobalLabels;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.Address;
import fr.wonder.ahk.transpilers.common_x64.addresses.ImmediateValue;
import fr.wonder.ahk.transpilers.common_x64.addresses.LabelAddress;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.ahk.transpilers.common_x64.instructions.OperationParameter;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnreachableException;

public class MemoryManager {
	
	private final AbstractWriter writer;
	private Scope currentScope;
	
	public MemoryManager(AbstractWriter writer, FunctionArgumentsLayout sectionArguments, int sectionStackSpace) {
		this.writer = writer;
		this.currentScope = new Scope(writer.unitWriter, sectionArguments, sectionStackSpace);
	}
	
	public void updateScope(boolean beginNewScope) {
		if(beginNewScope)
			currentScope.beginScope();
		else
			currentScope.endScope();
	}
	
	/** Used to offset all uses of $rsp, to revert pass a negative space */
	public void addStackOffset(int argsSpace) {
		currentScope.addStackOffset(argsSpace);
	}
	
	/**
	 * Writes the given expression value to #loc<br>
	 * If #exp is an instance of {@link NoneExp}, 0 is written, with a size cast if necessary.
	 */
	public void writeTo(Address loc, Expression exp, ErrorWrapper errors) {
		if(exp instanceof NoneExp) {
			// note: "NONE" is a nasm macro that resolve to "0"
			writeMov(loc, "NONE");
		} else if(exp instanceof LiteralExp) {
			writeMov(loc, writer.unitWriter.getValueString((LiteralExp<?>) exp));
		} else if(exp instanceof VarExp) {
			moveData(loc, currentScope.getVarAddress(((VarExp) exp).declaration));
		} else {
			writer.expWriter.writeExpression(exp, errors);
			if(loc != Register.RAX)
				moveData(loc, Register.RAX);
		}
	}
	
	/** Moves a literal to #loc, literals being ints, floats, string labels... */
	private void writeMov(Address loc, String literal) {
		if(loc instanceof Register && literal.equals("0") || literal.equals("0x0")) {
			writer.instructions.clearRegister((Register) loc);
		} else if(loc instanceof Register || loc instanceof LabelAddress) {
			writer.instructions.mov(loc, literal);
		} else if(loc instanceof MemAddress) {
			moveData(loc, new ImmediateValue(literal));
		} else {
			throw new IllegalStateException("Unhandled location type " + loc.getClass());
		}
	}
	
	/**
	 * Declares the variable to the current scope and writes its default value to its location.
	 * If the declaration does not have a default value a {@link NoneExp} is passed to {@link #writeTo(VarLocation, Expression, ErrorWrapper)}
	 */
	public MemAddress writeDeclaration(VariableDeclaration st, ErrorWrapper errors) {
		MemAddress loc = currentScope.declareVariable(st);
		writeTo(loc, st.getDefaultValue(), errors);
		return loc;
	}
	
	public void declareDummyStackVariable(String debugName) {
		currentScope.declareVariable(new DummyVariableDeclaration(debugName));
	}
	
	public Address getVarAddress(VarAccess declaration) {
		return currentScope.getVarAddress(declaration);
	}
	
	/**
	 * Writes the consecutive {@code mov} instructions to move any data stored at #from to #to.
	 * If both addresses are memory addresses, {@code rax} is used as a temporary storage,
	 * otherwise a single {@code mov} is enough.
	 */
	public void moveData(Address to, OperationParameter from) {
		if(from instanceof MemAddress && ((MemAddress) from).base instanceof MemAddress) {
			MemAddress f = (MemAddress) from;
			moveData(Register.RAX, f.base);
			from = new MemAddress(Register.RAX, f.index, f.scale, f.offset);
		}
		if(to instanceof MemAddress && ((MemAddress) to).base instanceof MemAddress) {
			MemAddress t = (MemAddress) to;
			moveData(Register.RBX, t.base);
			to = new MemAddress(Register.RBX, t.index, t.scale, t.offset);
		}
		if(from instanceof MemAddress && to instanceof MemAddress) {
			moveData(Register.RAX, from);
			from = Register.RAX;
		}
		
		
		if(from instanceof ImmediateValue && to instanceof MemAddress) {
			//  mov mem,imm64  does not exist, we must use rax to pass data
			// TODO find a way to separate imm32 and imm64
			moveData(Register.RAX, from);
			moveData(to, Register.RAX);
		} else {
			writer.instructions.mov(to, from);
		}
	}

	public void writeAffectationTo(Expression variable, Expression value, ErrorWrapper errors) {
		if(variable instanceof VarExp) {
			writeTo(currentScope.getVarAddress(((VarExp) variable).declaration), value, errors);
			
		} else if(variable instanceof IndexingExp) {
			IndexingExp exp = (IndexingExp) variable;
			writeTo(Register.RCX, value, errors);
			writer.instructions.push(Register.RCX);
			addStackOffset(8);
			Expression[] indices = exp.getIndices();
			if(indices.length > 1) {
				IndexingExp subExp = exp.subIndexingExpression();
				writeTo(Register.RAX, subExp, errors);
			} else {
				writeTo(Register.RAX, exp.getArray(), errors);
			}
			writeArrayAffectation(indices[indices.length-1], errors);
			addStackOffset(-8);
			
		} else if(variable instanceof DirectAccessExp) {
			DirectAccessExp exp = (DirectAccessExp) variable;
			ConcreteType structType = writer.unitWriter.types.getConcreteType(exp.getStructType().structure);
			writer.expWriter.writeExpression(exp.getStruct(), errors);
			writer.instructions.push(Register.RAX);
			addStackOffset(8);
			int memberOffset = structType.getOffset(exp.memberName);
			Address memberAddress = new MemAddress(new MemAddress(Register.RSP), memberOffset);
			writeTo(memberAddress, value, errors);
			writer.instructions.pop();
			addStackOffset(-8);
			
		} else {
			throw new UnreachableException("Cannot affect a value to type " + variable.getType().getName());
		}
	}
	
	private void writeArrayAffectation(Expression index, ErrorWrapper errors) {
		String oobLabel = writer.unitWriter.getSpecialLabel();
		if(index instanceof IntLiteral && ((IntLiteral) index).value == -1) {
			writer.instructions.mov(Register.RBX, new MemAddress(Register.RAX, -8));
			writer.instructions.pop(Register.RCX);
			writer.instructions.test(Register.RBX);
			writer.instructions.add(OpCode.JZ, oobLabel);
			writer.instructions.mov(new MemAddress(Register.RAX, Register.RBX, 1, -8), Register.RCX);
		} else {
			writer.instructions.push(Register.RAX);
			writer.mem.addStackOffset(MemSize.POINTER_SIZE);
			writer.mem.writeTo(Register.RBX, index, errors);
			writer.instructions.pop(Register.RAX);
			writer.mem.addStackOffset(-MemSize.POINTER_SIZE);
			writer.instructions.add(OpCode.SHL, Register.RBX, 3);
			writer.instructions.test(Register.RBX);
			writer.instructions.pop(Register.RCX);
			writer.instructions.add(OpCode.JS, oobLabel);
			writer.instructions.cmp(Register.RBX, new MemAddress(Register.RAX, -8));
			writer.instructions.add(OpCode.JGE, oobLabel);
			writer.instructions.mov(new MemAddress(Register.RAX, Register.RBX, 1), Register.RCX);
		}
		writer.instructions.label(oobLabel);
	}
	
	public void writeMultipleAffectationTo(Expression[] variables, Expression[] values, ErrorWrapper errors) {
		int stackSpace = variables.length*2*MemSize.POINTER_SIZE;
		addStackOffset(stackSpace);
		writer.instructions.add(OpCode.SUB, Register.RSP, stackSpace);
		
		Address[] variableAccesses = new Address[variables.length];
		
		for(int i = 0; i < variables.length; i++) {
			Address varAdd = new MemAddress(Register.RSP, i*2 * MemSize.POINTER_SIZE);
			Address valAdd = new MemAddress(Register.RSP, (i*2+1) * MemSize.POINTER_SIZE);
			writeTo(valAdd, values[i], errors);
			
			Expression variable = variables[i];
			if(variable instanceof VarExp) {
				variableAccesses[i] = currentScope.getVarAddress(((VarExp) variable).declaration);
				
			} else if(variable instanceof IndexingExp) {
				IndexingExp exp = (IndexingExp) variable;
				Expression[] indices = exp.getIndices();
				if(indices.length > 1) {
					IndexingExp subExp = exp.subIndexingExpression();
					writeTo(Register.RAX, subExp, errors);
				} else {
					writeTo(Register.RAX, exp.getArray(), errors);
				}
				Expression index = indices[indices.length-1];
				String oobLabel = writer.unitWriter.getSpecialLabel();
				writer.instructions.comment("writing " + exp);
				if(index instanceof IntLiteral && ((IntLiteral) index).value == -1) {
					writer.instructions.mov(Register.RBX, new MemAddress(Register.RAX, -8));
					writer.instructions.mov(Register.RDX, writer.unitWriter.requireExternLabel(GlobalLabels.GLOBAL_VOID));
					writer.instructions.test(Register.RBX);
					writer.instructions.add(OpCode.JZ, oobLabel);
					writer.instructions.lea(Register.RDX, new MemAddress(Register.RAX, Register.RBX, 1, -8));
				} else {
					writer.instructions.push(Register.RAX);
					writer.mem.addStackOffset(MemSize.POINTER_SIZE);
					writer.mem.writeTo(Register.RBX, index, errors);
					writer.instructions.pop(Register.RAX);
					writer.mem.addStackOffset(-MemSize.POINTER_SIZE);
					writer.instructions.add(OpCode.SHL, Register.RBX, 3);
					writer.instructions.mov(Register.RDX, writer.unitWriter.requireExternLabel(GlobalLabels.GLOBAL_VOID));
					writer.instructions.test(Register.RBX);
					writer.instructions.add(OpCode.JS, oobLabel);
					writer.instructions.cmp(Register.RBX, new MemAddress(Register.RAX, -8));
					writer.instructions.add(OpCode.JGE, oobLabel);
					writer.instructions.lea(Register.RDX, new MemAddress(Register.RAX, Register.RBX, 1));
				}
				writer.instructions.label(oobLabel);
				moveData(varAdd, Register.RDX);
				variableAccesses[i] = new MemAddress(varAdd);
				
			} else if(variable instanceof DirectAccessExp) {
				DirectAccessExp exp = (DirectAccessExp) variable;
				ConcreteType structType = writer.unitWriter.types.getConcreteType(exp.getStructType().structure);
				writeTo(varAdd, exp.getStruct(), errors);
				variableAccesses[i] = new MemAddress(varAdd, structType.getOffset(exp.memberName));
				
			} else {
				throw new UnreachableException("Cannot affect a value to type " + variable.getType().getName());
			}
		}
		
		for(int i = 0; i < variables.length; i++) {
			Address valAdd = new MemAddress(Register.RSP, (i*2+1) * MemSize.POINTER_SIZE);
			moveData(variableAccesses[i], valAdd);
		}
		
		addStackOffset(-stackSpace);
		writer.instructions.add(OpCode.ADD, Register.RSP, stackSpace);
	}
	
}
