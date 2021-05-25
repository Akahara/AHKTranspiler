package fr.wonder.ahk.transpilers.asm_x64.writers.memory;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.BoolLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.FloatLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.StrLiteral;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.statements.ForSt;
import fr.wonder.ahk.compiled.statements.SectionEndSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.transpilers.asm_x64.writers.FunctionWriter;
import fr.wonder.ahk.transpilers.asm_x64.writers.NoneExp;
import fr.wonder.ahk.transpilers.asm_x64.writers.UnitWriter;
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
	public final Scope unitScope;
	
	private FunctionScope functionScope;
	private Scope currentScope;
	
	private List<Integer> stackOffsets = new ArrayList<>();
	
	public MemoryManager(UnitWriter writer) {
		this.writer = writer;
		this.unitScope = new UnitScope(writer.unit);
		// until a function begins, the current scope is the global scope
		this.currentScope = unitScope;
	}
	
	public void enterFunction(FunctionSection func) {
		functionScope = new FunctionScope(func, unitScope);
		currentScope = functionScope;
	}
	
	public void updateScope(Statement st) {
		// update actual scope
		if(st instanceof SectionEndSt)
			currentScope = currentScope.getParent();
		else if(FunctionWriter.SECTION_STATEMENTS.contains(st.getClass()))
			currentScope = new SectionScope(currentScope);
		// update scope variables
		if(st instanceof VariableDeclaration)
			currentScope.declareVariable((VariableDeclaration) st);
		else if(st instanceof ForSt)
			currentScope.declareVariable(((ForSt) st).declaration);
	}
	
	/** Used to offset all uses of $rsp */
	public void addStackOffset(int argsSpace) {
		stackOffsets.add(argsSpace);
		currentScope.addStackOffset(argsSpace);
	}
	/** Used to remove the offset of $rsp. */
	public void restoreStackOffset() {
		currentScope.addStackOffset(-stackOffsets.remove(stackOffsets.size()-1));
	}
	
	/**
	 * Writes the given expression value to #loc<br>
	 * If #exp is an instance of {@link NoneExp}, 0 is written, with a size cast if necessary.
	 */
	public void writeTo(Address loc, Expression exp, ErrorWrapper errors) {
		if(exp instanceof NoneExp) {
			writeMov(loc, "NONE", writer.types.getSize(exp.getType()));
		} else if(exp instanceof LiteralExp) {
			writeMov(loc, getValueString((LiteralExp<?>) exp), MemSize.getPointerSize(exp.getType()).bytes);
		} else if(exp instanceof VarExp) {
			moveData(currentScope.getVarAddress(((VarExp) exp).declaration), loc);
		} else {
			writer.expWriter.writeExpression(exp, errors);
			if(loc != Register.RAX)
				moveData(Register.RAX, loc);
		}
	}
	
	/** Moves a literal to #loc, literals being ints, floats, string labels... */
	private void writeMov(Address loc, String literal, int literalSize) {
		if(loc instanceof Register && literal.equals("0") || literal.equals("0x0")) {
			writer.instructions.clearRegister((Register) loc);
		} else if(loc instanceof Register || loc instanceof LabelAddress) {
			writer.instructions.mov(loc, new ImmediateValue(literal));
		} else if(loc instanceof MemAddress) {
			writer.instructions.mov(loc, new ImmediateValue(literal), MemSize.getSize(literalSize));
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
	
	/**
	 * moves the given expression value to #loc if it is not a literal nor a variable expression,
	 * returns the location where #exp was stored (either #loc, the location of the variable or the raw literal)
	 */
	public OperationParameter moveTo(Address loc, Expression exp, ErrorWrapper errors) {
		if(exp instanceof LiteralExp) {
			return new ImmediateValue(((LiteralExp<?>) exp).toString());
		} else if(exp instanceof VarExp) {
			return currentScope.getVarAddress(((VarExp) exp).declaration);
		} else {
			writeTo(loc, exp, errors);
			return loc;
		}
	}
	
	/**
	 * Writes the consecutive {@code mov} instructions to move any data stored at #from to #to.
	 * If both addresses are memory addresses, {@code rax} is used as a temporary storage,
	 * otherwise a single {@code mov} is enough.
	 */
	public void moveData(Address from, Address to) {
		if(from instanceof MemAddress && ((MemAddress) from).base instanceof MemAddress) {
			MemAddress f = (MemAddress) from;
			moveData(f.base, Register.RAX);
			from = new MemAddress(Register.RAX, f.index, f.scale, f.offset);
		}
		if(to instanceof MemAddress && ((MemAddress) to).base instanceof MemAddress) { // FIX changing RBX may be a big problem
			MemAddress t = (MemAddress) to;
			moveData(t.base, Register.RBX);
			from = new MemAddress(Register.RBX, t.index, t.scale, t.offset);
		}
		if(from instanceof MemAddress && to instanceof MemAddress) {
			moveData(from, Register.RAX);
			from = Register.RAX;
		}
		writer.instructions.mov(to, from);
		// FIX TEST moving data from complex mem to complex mem
	}
	
	/**
	 * Returns the assembly text corresponding to a literal expression
	 * <ul>
	 *   <li><b>Ints</b> are not converted</li>
	 *   <li><b>Floats</b> are converted using the {@code __float64__} directive</li>
	 *   <li><b>Bools</b> are converted to 0 or 1</li>
	 *   <li><b>Strings</b> are converted to their labels in the data segment</li>
	 * </ul>
	 */
	public String getValueString(LiteralExp<?> exp) {
		if(exp instanceof IntLiteral)
			return String.valueOf(((IntLiteral) exp).value);
		else if(exp instanceof FloatLiteral)
			return "__float64__("+((FloatLiteral)exp).value+")";
		else if(exp instanceof BoolLiteral)
			return ((BoolLiteral) exp).value ? "1" : "0";
		else if(exp instanceof StrLiteral)
			return writer.getLabel((StrLiteral) exp);
		else
			throw new IllegalStateException("Unhandled literal type " + exp.getClass());
	}

}
