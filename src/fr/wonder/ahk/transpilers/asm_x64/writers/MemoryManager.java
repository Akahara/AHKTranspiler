package fr.wonder.ahk.transpilers.asm_x64.writers;

import fr.wonder.ahk.compiled.expressions.Expression;
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
			writeMov(loc, "NONE", MemSize.getSize(writer.types.getSize(exp.getType())));
		} else if(exp instanceof LiteralExp) {
			writeMov(loc, writer.getValueString((LiteralExp<?>) exp), MemSize.getPointerSize(exp.getType()));
		} else if(exp instanceof VarExp) {
			moveData(currentScope.getVarAddress(((VarExp) exp).declaration), loc);
		} else {
			writer.expWriter.writeExpression(exp, errors);
			if(loc != Register.RAX)
				moveData(Register.RAX, loc);
		}
	}
	
	/** Moves a literal to #loc, literals being ints, floats, string labels... */
	private void writeMov(Address loc, String literal, MemSize literalSize) {
		if(loc instanceof Register && literal.equals("0") || literal.equals("0x0")) {
			writer.instructions.clearRegister((Register) loc);
		} else if(loc instanceof Register || loc instanceof LabelAddress) {
			writer.instructions.mov(loc, literal);
		} else if(loc instanceof MemAddress) {
			moveData(new ImmediateValue(literal), loc, literalSize);
//			writer.instructions.mov(loc, literal, MemSize.getSize(literalSize));
		} else {
			throw new IllegalStateException("Unhandled location type " + loc.getClass());
		}
	}
	
	/**
	 * stores the value of #exp in the location of #var
	 * (this can only be done here as other classes cannot know where #var is stored)
	 */
	public void writeTo(VarAccess var, Expression exp, ErrorWrapper errors) {
		writeTo(currentScope.getVarAddress(var), exp, errors);
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
	
	public void moveData(OperationParameter from, Address to) {
		moveData(from, to, null);
	}

	/**
	 * Writes the consecutive {@code mov} instructions to move any data stored at #from to #to.
	 * If both addresses are memory addresses, {@code rax} is used as a temporary storage,
	 * otherwise a single {@code mov} is enough.
	 */
	public void moveData(OperationParameter from, Address to, MemSize cast) {
		if(from instanceof MemAddress && ((MemAddress) from).base instanceof MemAddress) {
			MemAddress f = (MemAddress) from;
			moveData(f.base, Register.RAX);
			from = new MemAddress(Register.RAX, f.index, f.scale, f.offset);
		}
		if(to instanceof MemAddress && ((MemAddress) to).base instanceof MemAddress) { // FIX changing RBX may be a big problem
			MemAddress t = (MemAddress) to;
			moveData(t.base, Register.RBX);
			to = new MemAddress(Register.RBX, t.index, t.scale, t.offset);
		}
		if(from instanceof MemAddress && to instanceof MemAddress) {
			moveData(from, Register.RAX);
			from = Register.RAX;
		}
		writer.instructions.mov(to, from, cast);
		// FIX TEST moving data from complex mem to complex mem
	}
	
}
