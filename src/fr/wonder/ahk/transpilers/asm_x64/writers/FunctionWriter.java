package fr.wonder.ahk.transpilers.asm_x64.writers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.wonder.ahk.compiled.statements.AffectationSt;
import fr.wonder.ahk.compiled.statements.ElseSt;
import fr.wonder.ahk.compiled.statements.ForSt;
import fr.wonder.ahk.compiled.statements.FunctionSt;
import fr.wonder.ahk.compiled.statements.IfSt;
import fr.wonder.ahk.compiled.statements.LabeledStatement;
import fr.wonder.ahk.compiled.statements.RangedForSt;
import fr.wonder.ahk.compiled.statements.ReturnSt;
import fr.wonder.ahk.compiled.statements.SectionEndSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.statements.WhileSt;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class FunctionWriter {
	
	public static final List<Class<? extends Statement>> SECTION_STATEMENTS = Arrays.asList(
			IfSt.class, ElseSt.class, ForSt.class, RangedForSt.class, WhileSt.class
	);
	
	private final UnitWriter writer;
	/** list of labels local to the function (all are starting with a dot) */
	private final Map<Statement, String> labelsMap = new HashMap<>();
	
	private int debugLabelIndex = 0;
	
	public FunctionWriter(UnitWriter writer, FunctionSection func) {
		this.writer = writer;
		// fill the labels map
		int l = 0;
		for(Statement s : func.body) {
			if(s instanceof LabeledStatement) {
				String name = s.getClass().getSimpleName();
				name = name.substring(0, name.length()-2).toLowerCase();
				String label = "." + name + "_" + (l++);
				labelsMap.put(s, label);
			} else if(s instanceof SectionEndSt) {
				// the label of the closed statement has already been set
				labelsMap.put(s, ".end_"+labelsMap.get(((SectionEndSt) s).closedStatement).substring(1));
			}
		}
	}
	
	public void writeFunction(FunctionSection func, ErrorWrapper errors) {
		writer.instructions.createStackFrame();
		
		int stackSpace = getMaxStackSize(func.body);
		if(stackSpace != 0)
			writer.instructions.add(OpCode.SUB, Register.RSP, stackSpace);
		
		writer.mem.enterFunction(func, stackSpace);
		
		int argsSpace = getArgumentsSize(func.getPrototype());
		
		boolean needsRetLabel = false;
		
		for(int i = 0; i < func.body.length; i++) {
			Statement st = func.body[i];
			boolean scopeUpdated = false;
			
			if(writer.project.manifest.DEBUG_SYMBOLS) {
				if(st.sourceStop != -1)
					writer.instructions.comment(st.getSource().getLine(st).strip());
				else
					writer.instructions.comment("~ " + st.toString());
			}
			writer.instructions.label(".dbg_"+(debugLabelIndex++));
			
			if(st instanceof SectionEndSt) {
				writeSectionEndStatement((SectionEndSt) st, errors);
				
			} else if(st instanceof VariableDeclaration) {
				writeVarDeclaration((VariableDeclaration) st, errors);
				
			} else if(st instanceof IfSt) {
				writeIfStatement((IfSt) st, errors);
				
			} else if(st instanceof ElseSt) {
				writeElseStatement((ElseSt) st, errors);
				
			} else if(st instanceof WhileSt) {
				writeWhileStatement((WhileSt) st, errors);
				
			} else if(st instanceof ForSt) {
				writer.mem.updateScope(st);
				scopeUpdated = true;
				writeForStatement((ForSt) st, errors);
				
			} else if(st instanceof RangedForSt) {
				writer.mem.updateScope(st);
				scopeUpdated = true;
				writeRangedForStatement((RangedForSt) st, errors);
				
			} else if(st instanceof ReturnSt) {
				writeReturnStatement((ReturnSt) st, i != func.body.length-1, errors);
				needsRetLabel |= i != func.body.length-1;
				
			} else if(st instanceof FunctionSt) {
				writeFunctionStatement(((FunctionSt) st), errors);
				
			} else if(st instanceof AffectationSt) {
				writeAffectationStatement((AffectationSt) st, errors);
				
			} else {
				errors.add("Unhandled statement type: " + st.getClass().getSimpleName() + " " + st.getErr());
			}
			
			if(!scopeUpdated)
				writer.mem.updateScope(st);
		}
		
		if(needsRetLabel)
			writer.instructions.label(".ret");
		writer.instructions.endStackFrame();
		switch(writer.project.manifest.callingConvention) {
		case __stdcall:
			writer.instructions.ret(argsSpace);
			break;
		default:
			throw new IllegalStateException("Unimplemented calling convention " + writer.project.manifest.callingConvention);
		}
	}
	
	private static int getMaxStackSize(Statement[] statements) {
		List<Integer> sections = new ArrayList<>();
		int current = 0;
		int max = 0;
		for(Statement s : statements) {
			if(s instanceof SectionEndSt) {
				current = sections.remove(sections.size()-1);
			} else if(s instanceof VariableDeclaration) {
				current += MemSize.POINTER_SIZE;
				max = Math.max(current, max);
			} else if(s instanceof ForSt && ((ForSt) s).declaration != null) {
				current += MemSize.POINTER_SIZE;
				max = Math.max(current, max);
				sections.add(current);
			} else if(s instanceof RangedForSt) {
				current += MemSize.POINTER_SIZE;
				max = Math.max(current, max);
				sections.add(current);
			} else if(SECTION_STATEMENTS.contains(s.getClass())) {
				sections.add(current);
			}
		}
		return max;
	}
	
	public static int getArgumentsSize(FunctionPrototype func) {
		return func.functionType.arguments.length * MemSize.POINTER_SIZE;
	}
	
	private String getLabel(Statement st) {
		String label = labelsMap.get(st);
		if(label == null)
			throw new IllegalStateException("Statement is unlabeled! " + st);
		return label;
	}
	
	private void writeSectionEndStatement(SectionEndSt st, ErrorWrapper errors) {
		if(st.closedStatement instanceof ForSt || st.closedStatement instanceof RangedForSt)
			writer.instructions.jmp(getLabel(st.closedStatement));
		else if(st.closedStatement instanceof IfSt && ((IfSt)st.closedStatement).elseStatement != null)
			writer.instructions.jmp(getLabel(((IfSt)st.closedStatement).elseStatement.sectionEnd));
		else if(st.closedStatement instanceof WhileSt)
			writer.instructions.jmp(getLabel(st.closedStatement));
		writer.instructions.label(getLabel(st));
	}
	
	private void writeVarDeclaration(VariableDeclaration st, ErrorWrapper errors) {
		writer.mem.writeDeclaration(st, errors);
	}
	
	private void writeIfStatement(IfSt st, ErrorWrapper errors) {
		writer.opWriter.writeJump(st.getCondition(), getLabel(st.sectionEnd), errors);
	}
	
	private void writeElseStatement(ElseSt st, ErrorWrapper errors) {
		
	}
	
	private void writeWhileStatement(WhileSt st, ErrorWrapper errors) {
		writer.instructions.label(getLabel(st));
		writer.opWriter.writeJump(st.getCondition(), getLabel(st.sectionEnd), errors);
	}
	
	private void writeForStatement(ForSt st, ErrorWrapper errors) {
		writeForStatement(st, getLabel(st), errors);
	}
	
	private void writeRangedForStatement(RangedForSt st, ErrorWrapper errors) {
		writeForStatement(st.toComplexFor(), getLabel(st), errors); // FIX compute step and max beforehand
	}
	
	/**
	 * @param label the label associated with the ForSt or RangedForSt statement
	 *              (note that when using {@link RangedForSt#toComplexFor()} the
	 *              returned ForSt does NOT have a label)
	 */
	private void writeForStatement(ForSt st, String label, ErrorWrapper errors) {
		if(st.declaration != null)
			writeVarDeclaration(st.declaration, errors);
		if(st.affectation != null)
			writer.instructions.jmp(label + "_firstpass");
		writer.instructions.label(label);
		if(st.affectation != null) {
			writeAffectationStatement(st.affectation, errors);
			writer.instructions.label(label + "_firstpass");
		}
		if(st.condition != null) {
			writer.opWriter.writeJump(st.condition, getLabel(st.sectionEnd), errors);
		} else {
			writer.instructions.jmp(label);
		}
	}
	
	private void writeReturnStatement(ReturnSt st, boolean writeJmp, ErrorWrapper errors) {
		writer.expWriter.writeExpression(st.getExpression(), errors);
		if(writeJmp)
			writer.instructions.jmp(".ret");
	}
	
	private void writeFunctionStatement(FunctionSt st, ErrorWrapper errors) {
		writer.expWriter.writeExpression(st.getFunction(), errors);
	}
	
	private void writeAffectationStatement(AffectationSt st, ErrorWrapper errors) {
		writer.mem.writeAffectationTo(st.getVariable(), st.getValue(), errors);
	}
	
}
