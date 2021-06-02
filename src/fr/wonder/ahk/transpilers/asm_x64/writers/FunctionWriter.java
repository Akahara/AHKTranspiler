package fr.wonder.ahk.transpilers.asm_x64.writers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.AffectationSt;
import fr.wonder.ahk.compiled.statements.ElseSt;
import fr.wonder.ahk.compiled.statements.ForSt;
import fr.wonder.ahk.compiled.statements.FunctionSt;
import fr.wonder.ahk.compiled.statements.IfSt;
import fr.wonder.ahk.compiled.statements.LabeledStatement;
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
			IfSt.class, ElseSt.class, ForSt.class, WhileSt.class
	);
	
	private final UnitWriter writer;
	/** list of labels local to the function (all are starting with a dot) */
	private final Map<Statement, String> labelsMap = new HashMap<>();
	
	public FunctionWriter(UnitWriter writer, FunctionSection func) {
		this.writer = writer;
		// fill the labels map
		int l = 0;
		for(Statement s : func.body) {
			if(s instanceof LabeledStatement) {
				String name = s.getClass().getSimpleName();
				name = name.substring(0, name.length()-2).toLowerCase();
				String label = "." + name + "@" + (l++);
				labelsMap.put(s, label);
			} else if(s instanceof SectionEndSt) {
				// the label of the closed statement has already been set
				labelsMap.put(s, ".end_"+labelsMap.get(((SectionEndSt) s).closedStatement).substring(1));
			}
		}
	}
	
	public void writeFunction(FunctionSection func, ErrorWrapper errors) {
		writer.instructions.createScope();
		
		int stackSpace = getMaxStackSize(func.body);
		if(stackSpace != 0)
			writer.instructions.add(OpCode.SUB, Register.RSP, stackSpace);
		
		writer.mem.enterFunction(func, stackSpace);
		
		int argsSpace = getArgumentsSize(func.getPrototype());
		
		boolean needsRetLabel = false;
		
		for(int i = 0; i < func.body.length; i++) {
			Statement st = func.body[i];
			boolean scopeUpdated = false;
			
			// TODO0 remove or keep the code lines in assembly as a debug setting
			writer.instructions.comment(st.getSource().getLine(st).strip());
			
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
		writer.instructions.endScope();
		switch(writer.projectHandle.manifest.callingConvention) {
		case __stdcall:
			writer.instructions.ret(argsSpace);
			break;
		default:
			throw new IllegalStateException("Unimplemented calling convention " + writer.projectHandle.manifest.callingConvention);
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
				current += MemSize.getPointerSize(((VariableDeclaration) s).getType()).bytes;
				max = Math.max(current, max);
			} else if(s instanceof ForSt && ((ForSt) s).declaration != null) {
				current += MemSize.getPointerSize(((ForSt) s).declaration.getType()).bytes;
				max = Math.max(current, max);
				sections.add(current);
			} else if(SECTION_STATEMENTS.contains(s.getClass())) {
				sections.add(current);
			}
		}
		return max;
	}
	
	public static int getArgumentsSize(FunctionPrototype func) {
		int size = 0;
		for(VarType arg : func.functionType.arguments)
			size += MemSize.getPointerSize(arg).bytes;
		return size;
	}
	
	private String getLabel(Statement st) {
		String label = labelsMap.get(st);
		if(label == null)
			throw new IllegalStateException("Statement is unlabeled! " + st);
		return label;
	}
	
	private void writeSectionEndStatement(SectionEndSt st, ErrorWrapper errors) {
		if(st.closedStatement instanceof ForSt)
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
		if(!writer.asmWriter.writeJump(st.getCondition(), getLabel(st.sectionEnd), errors)) {
			writer.expWriter.writeExpression(st.getCondition(), errors);
			writer.instructions.add(OpCode.JZ, getLabel(st.sectionEnd));
		}
	}
	
	private void writeElseStatement(ElseSt st, ErrorWrapper errors) {
		
	}
	
	private void writeWhileStatement(WhileSt st, ErrorWrapper errors) {
		String label = getLabel(st);
		writer.instructions.label(label);
		writer.asmWriter.writeJump(st.getCondition(), getLabel(st.sectionEnd), errors);
	}
	
	private void writeForStatement(ForSt st, ErrorWrapper errors) {
		String label = getLabel(st);
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
			if(!writer.asmWriter.writeJump(st.condition, getLabel(st.sectionEnd), errors)) {
				writer.expWriter.writeExpression(st.condition, errors);
				writer.instructions.add(OpCode.JZ, getLabel(st.sectionEnd));
			}
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
		// FIX compute the path st is pointing to and write to it
		// currently this method only works for variables (not for arrays, structs ...)
		writer.mem.writeTo(((VarExp)st.getVariable()).declaration, st.getValue(), errors);
	}
	
}
