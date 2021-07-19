package fr.wonder.ahk.transpilers.asm_x64.writers;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.IndexingExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.statements.SectionEndSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.Address;
import fr.wonder.ahk.transpilers.common_x64.addresses.ImmediateValue;
import fr.wonder.ahk.transpilers.common_x64.addresses.LabelAddress;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;
import fr.wonder.ahk.transpilers.common_x64.instructions.OperationParameter;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class MemoryManager {
	
	private final UnitWriter writer;
	private Scope currentScope;
	
	public MemoryManager(UnitWriter writer) {
		this.writer = writer;
	}
	
	public void enterFunction(FunctionSection func, int stackSpace) {
		currentScope = new Scope(writer.unit, func, stackSpace);
	}
	
	public void updateScope(Statement st) {
		if(st instanceof SectionEndSt)
			currentScope.endScope();
		else if(FunctionWriter.SECTION_STATEMENTS.contains(st.getClass()))
			currentScope.beginScope();
	}
	
	/** Used to offset all uses of $rsp */
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
			writeMov(loc, "NONE", MemSize.POINTER);
		} else if(exp instanceof LiteralExp) {
			writeMov(loc, writer.getValueString((LiteralExp<?>) exp), MemSize.POINTER);
		} else if(exp instanceof VarExp) {
			moveData(loc, currentScope.getVarAddress(((VarExp) exp).declaration));
		} else {
			writer.expWriter.writeExpression(exp, errors);
			if(loc != Register.RAX)
				moveData(loc, Register.RAX);
		}
	}
	
	/**
	 * stores the value of #exp in the location of #var
	 * (this can only be done here as other classes cannot know where #var is stored)
	 */
	public void writeTo(VarAccess var, Expression exp, ErrorWrapper errors) {
		writeTo(currentScope.getVarAddress(var), exp, errors);
	}
	
	/** Moves a literal to #loc, literals being ints, floats, string labels... */
	private void writeMov(Address loc, String literal, MemSize literalSize) {
		if(loc instanceof Register && literal.equals("0") || literal.equals("0x0")) {
			writer.instructions.clearRegister((Register) loc);
		} else if(loc instanceof Register || loc instanceof LabelAddress) {
			writer.instructions.mov(loc, literal);
		} else if(loc instanceof MemAddress) {
			moveData(loc, new ImmediateValue(literal), literalSize);
		} else {
			throw new IllegalStateException("Unhandled location type " + loc.getClass());
		}
	}
	
	/**
	 * Declares the variable to the current scope and writes its default value to its location.
	 * If the declaration does not have a default value a {@link NoneExp} is passed to {@link #writeTo(VarLocation, Expression, ErrorWrapper)}
	 */
	public void writeDeclaration(VariableDeclaration st, ErrorWrapper errors) {
		Address loc = currentScope.declareVariable(st);
		writeTo(loc, st.getDefaultValue(), errors);
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
		moveData(to, from, null);
	}

	/**
	 * Writes the consecutive {@code mov} instructions to move any data stored at #from to #to.
	 * If both addresses are memory addresses, {@code rax} is used as a temporary storage,
	 * otherwise a single {@code mov} is enough.
	 */
	public void moveData(Address to, OperationParameter from, MemSize cast) {
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
			moveData(Register.RAX, from);
			moveData(to, Register.RAX);
		} else {
			writer.instructions.mov(to, from, cast);
		}
	}

	public void writeAffectationTo(Expression variable, Expression value, ErrorWrapper errors) {
		if(variable instanceof VarExp) {
			writeTo(((VarExp) variable).declaration, value, errors);
		} else if(variable instanceof IndexingExp) {
			// TODO this can be heavily optimized
			IndexingExp exp = (IndexingExp) variable;
			Expression[] indices = exp.getIndices();
			if(indices.length > 1) {
				IndexingExp subExp = exp.subIndexingExpression();
				writeTo(Register.RAX, subExp, errors);
			} else {
				writeTo(Register.RAX, exp.getArray(), errors);
			}
			writer.instructions.push(Register.RAX);
			addStackOffset(8);
			writeTo(Register.RAX, indices[indices.length-1], errors);
			writer.instructions.push(Register.RAX);
			addStackOffset(8);
			writeTo(Register.RCX, value, errors);  // value
			writer.instructions.pop(Register.RBX); // index
			writer.instructions.pop(Register.RAX); // array
			addStackOffset(-16);
			// TODO check for out of bounds affectations
			writer.instructions.mov(
					new MemAddress(Register.RAX, Register.RBX, MemSize.POINTER_SIZE),
					Register.RCX);
		} else {
			errors.add("Cannot affect a value to type " + variable.getType().getName() + variable.getErr());
		}
	}
	
}
