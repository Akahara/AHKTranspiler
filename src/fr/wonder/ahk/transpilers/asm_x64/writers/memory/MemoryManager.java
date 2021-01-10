package fr.wonder.ahk.transpilers.asm_x64.writers.memory;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.BoolLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.FloatLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.StrLiteral;
import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.statements.ForSt;
import fr.wonder.ahk.compiled.statements.SectionEndSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.transpilers.asm_x64.writers.FunctionWriter;
import fr.wonder.ahk.transpilers.asm_x64.writers.NoneExp;
import fr.wonder.ahk.transpilers.asm_x64.writers.UnitWriter;
import fr.wonder.ahk.utils.ErrorWrapper;

public class MemoryManager {
	
	/** The size (in bytes) of a pointer in 64 bits mode */
	public static final int POINTER_SIZE = 8;
	
	private final UnitWriter writer;
	private final UnitScope unitScope;
	
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
	public void writeTo(VarLocation loc, Expression exp, ErrorWrapper errors) {
		if(exp instanceof NoneExp) {
			writeMov(loc, "NONE", exp.getType().getSize());
		} else if(exp instanceof LiteralExp) {
			writeMov(loc, getValueString((LiteralExp<?>) exp), exp.getType().getSize());
		} else if(exp instanceof VarExp) {
			moveData(currentScope.getVarLocation(((VarExp) exp).declaration), loc);
		} else {
			writer.expWriter.writeExpression(exp, errors);
			if(loc != DirectLoc.LOC_RAX)
				moveData(DirectLoc.LOC_RAX, loc);
		}
	}
	
	/** Moves a literal to #loc, literals being ints, floats, string labels... */
	private void writeMov(VarLocation loc, String literal, int literalSize) {
		if(loc instanceof DirectLoc && literal.equals("0") || literal.equals("0x0")) {
			writer.buffer.writeLine("xor " + loc.getLoc() + "," + loc.getLoc());
		} else if(loc instanceof DirectLoc) {
			writer.buffer.writeLine("mov " + loc.getLoc() + "," + literal);
		} else if(loc instanceof MemoryLoc) {
			writer.buffer.writeLine("mov " + MemSize.getSize(literalSize).getCast() + " " + loc.getLoc() + "," + literal);
		} else if(loc instanceof ComplexLoc) {
			// TODO optimize a bit, there is no need of using RAX
			writeMov(DirectLoc.LOC_RAX, literal, literalSize);
			moveData(DirectLoc.LOC_RAX, loc);
		} else {
			throw new IllegalStateException("Unhandled location type " + loc.getClass().getSimpleName());
		}
	}
	
	/**
	 * stores the value of #exp in the location of #var
	 * (this can only be done here as other classes cannot know where #var is stored)
	 */
	public void writeTo(ValueDeclaration var, Expression exp, ErrorWrapper errors) {
		writeTo(currentScope.getVarLocation(var), exp, errors);
	}
	
	/**
	 * Writes either the literal or the expression of #acc to #loc, depending on which #acc
	 * has defined
	 */
	public void writeTo(VarLocation loc, DataAccess acc, ErrorWrapper errors) {
		if(acc.exp != null)
			writeTo(loc, acc.exp, errors);
		else
			moveData(acc.loc, loc);
	}

	/**
	 * Declares the variable to the current scope and writes its default value to its location.
	 * If the declaration does not have a default value a {@link NoneExp} is passed to {@link #writeTo(VarLocation, Expression, ErrorWrapper)}
	 */
	public void writeDeclaration(VariableDeclaration st, ErrorWrapper errors) {
		VarLocation loc = currentScope.declareVariable(st);
		Expression val = st.getDefaultValue() != null ? st.getDefaultValue() : new NoneExp(st.getType().getSize());
		writeTo(loc, val, errors);
	}
	
	/**
	 * moves the given expression value to #loc if it is not a literal nor a variable expression,
	 * returns the location where #exp was stored (either #loc, the location of the variable or the raw literal)
	 */
	public DataAccess moveTo(VarLocation loc, Expression exp, ErrorWrapper errors) {
		if(exp instanceof LiteralExp) {
			return new DataAccess((LiteralExp<?>) exp);
		} else if(exp instanceof VarExp) {
			return new DataAccess(currentScope.getVarLocation(((VarExp) exp).declaration));
		} else {
			writeTo(loc, exp, errors);
			return new DataAccess(loc);
		}
	}
	
	/**
	 * Writes the consecutive <code>mov</code> directives to move any data stored at #from to #to.
	 * <ul>
	 * <li>If both are {@link DirectLoc} the <code>rax</code> is used once to hold a temporary
	 * 		address and two <code>mov</code> directives are enough.
	 * <li>If any of #from and #to is a {@link DirectLoc} and neither are a {@link ComplexLoc}
	 * 		a single <code>mov</code> directive is enough.</li>
	 * <li>Otherwise (at least one is a {@link ComplexLoc}) the data is transfered using rax
	 * 		and/or rbx to hold temporary address(es) and the number of <code>mov</code> directive
	 * 		greatly increases.</li>
	 * </ul>
	 */
	public void moveData(VarLocation from, VarLocation to) {
		if(to instanceof MemoryLoc && from instanceof MemoryLoc) {
			// mem to mem
			moveData(from, DirectLoc.LOC_RAX);
			moveData(DirectLoc.LOC_RAX, to);
		} else if(!(to instanceof ComplexLoc) && !(from instanceof ComplexLoc)) {
			// direct mov (mem to direct or direct to mem)
			writer.buffer.writeLine("mov " + to.getLoc() + "," + from.getLoc());
		} else {
			// complex to any or any to complex
			if(to instanceof ComplexLoc) {
				ComplexLoc tc = (ComplexLoc) to;
				if(tc.offsets.length == 1) {
					to = new MemoryLoc(tc.base, tc.offsets[0]);
				} else {
					moveData(new MemoryLoc(tc.base, tc.offsets[0]), DirectLoc.LOC_RBX);
					for(int i = 1; i < tc.offsets.length-1; i++)
						moveData(new MemoryLoc(VarLocation.REG_RBX, tc.offsets[i]), DirectLoc.LOC_RBX);
					to = new MemoryLoc(VarLocation.REG_RBX, tc.offsets[tc.offsets.length-1]);
				}
			}
			if(from instanceof ComplexLoc) {
				ComplexLoc fc = (ComplexLoc) from;
				if(fc.offsets.length == 1) {
					from = new MemoryLoc(fc.base, fc.offsets[0]);
				} else {
					moveData(new MemoryLoc(fc.base, fc.offsets[0]), DirectLoc.LOC_RAX);
					for(int i = 1; i < fc.offsets.length-1; i++)
						moveData(new MemoryLoc(VarLocation.REG_RAX, fc.offsets[i]), DirectLoc.LOC_RAX);
					from = new MemoryLoc(VarLocation.REG_RAX, fc.offsets[fc.offsets.length-1]);
				}
			}
			moveData(from, to);
		}
	}

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
