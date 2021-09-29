package fr.wonder.ahk.transpilers.asm_x64.writers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.LiteralExp.BoolLiteral;
import fr.wonder.ahk.compiled.expressions.types.VarGenericType;
import fr.wonder.ahk.compiled.statements.AffectationSt;
import fr.wonder.ahk.compiled.statements.ElseSt;
import fr.wonder.ahk.compiled.statements.ForSt;
import fr.wonder.ahk.compiled.statements.FunctionSt;
import fr.wonder.ahk.compiled.statements.IfSt;
import fr.wonder.ahk.compiled.statements.LabeledStatement;
import fr.wonder.ahk.compiled.statements.MultipleAffectationSt;
import fr.wonder.ahk.compiled.statements.OperationSt;
import fr.wonder.ahk.compiled.statements.RangedForSt;
import fr.wonder.ahk.compiled.statements.ReturnSt;
import fr.wonder.ahk.compiled.statements.SectionEndSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.statements.WhileSt;
import fr.wonder.ahk.compiled.units.prototypes.blueprints.BlueprintPrototype;
import fr.wonder.ahk.compiled.units.prototypes.blueprints.GenericImplementationParameter;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.transpilers.asm_x64.writers.operations.AsmOperationWriter;
import fr.wonder.ahk.transpilers.common_x64.InstructionSet;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnimplementedException;

public class FunctionWriter {
	
	public final FunctionSection func;
	public final InstructionSet instructions;
	
	public final UnitWriter unitWriter;
	public final ExpressionWriter expWriter;
	public final MemoryManager mem;
	public final AsmOperationWriter opWriter;
	public final AsmClosuresWriter closureWriter;
	
	private final int stackSpace;
	
	/** list of labels local to the function (all are starting with a dot) */
	private final Map<Statement, String> labelsMap = new HashMap<>();
	
	private int debugLabelIndex = 0;
	
	public FunctionWriter(UnitWriter writer, FunctionSection func) {
		this.func = func;
		this.instructions = writer.instructions;
		this.unitWriter = writer;
		this.stackSpace = getMaxStackSize(func.body);
		this.expWriter = new ExpressionWriter(this);
		this.opWriter = new AsmOperationWriter(this);
		this.closureWriter = new AsmClosuresWriter(this);
		this.mem = new MemoryManager(this, stackSpace);
		fillLabelsMap();
	}

	public void writeGIPToRAX(VarGenericType genericInstance, BlueprintPrototype blueprint) {
		for(int i = 0; i < func.genericContext.gips.length; i++) {
			GenericImplementationParameter gip = func.genericContext.gips[i];
			if(gip.genericType.equals(genericInstance) &&
					gip.typeRequirement.blueprint.matchesPrototype(blueprint)) {
				instructions.mov(Register.RAX, new MemAddress(Register.RBP, 16+(func.arguments.length+i) * MemSize.POINTER_SIZE));
				return;
			}
		}
		throw new IllegalStateException("Blueprint " + genericInstance + ":" + blueprint + " is not implemented in function " + func);
	}

	public void writeFunction(ErrorWrapper errors) {
		instructions.createStackFrame();
		
		if(stackSpace != 0)
			instructions.add(OpCode.SUB, Register.RSP, stackSpace);
		
		int argsSpace = (func.arguments.length + func.genericContext.gips.length) * MemSize.POINTER_SIZE;
		
		boolean needsRetLabel = false;
		
		for(int i = 0; i < func.body.length; i++) {
			Statement st = func.body[i];
			boolean scopeUpdated = false;
			
			if(unitWriter.project.manifest.DEBUG_SYMBOLS) {
				if(st.sourceRef.stop != -1)
					instructions.comment(st.sourceRef.getLine().strip());
				else
					instructions.comment("~ " + st.toString());
			}
			instructions.label(".dbg_"+(debugLabelIndex++));
			
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
				mem.updateScope(true);
				scopeUpdated = true;
				writeForStatement((ForSt) st, errors);
				
			} else if(st instanceof RangedForSt) {
				mem.updateScope(true);
				scopeUpdated = true;
				writeRangedForStatement((RangedForSt) st, errors);
				
			} else if(st instanceof ReturnSt) {
				writeReturnStatement((ReturnSt) st, i != func.body.length-1, errors);
				needsRetLabel |= i != func.body.length-1;
				
			} else if(st instanceof FunctionSt) {
				writeFunctionStatement((FunctionSt) st, errors);
				
			} else if(st instanceof OperationSt) {
				writeOperationStatement((OperationSt) st, errors);
				
			} else if(st instanceof AffectationSt) {
				writeAffectationStatement((AffectationSt) st, errors);
				
			} else if(st instanceof MultipleAffectationSt) {
				writeMultipleAffectationSt((MultipleAffectationSt) st, errors);
				
			} else {
				throw new UnimplementedException("Unhandled statement type: " + st.getClass().getSimpleName());
			}
			
			if(!scopeUpdated) {
				if(st instanceof SectionEndSt)
					mem.updateScope(false);
				else if(st instanceof LabeledStatement)
					mem.updateScope(true);
			}
		}
		
		if(needsRetLabel)
			instructions.label(".ret");
		instructions.endStackFrame();
		instructions.ret(argsSpace);
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
				current += 3*MemSize.POINTER_SIZE;
				max = Math.max(current, max);
				sections.add(current);
			} else if(s instanceof LabeledStatement) {
				sections.add(current);
			}
		}
		return max;
	}
	
	private void fillLabelsMap() {
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
	
	private String getLabel(Statement st) {
		String label = labelsMap.get(st);
		if(label == null)
			throw new IllegalStateException("Statement is unlabeled! " + st);
		return label;
	}
	
	private void writeSectionEndStatement(SectionEndSt st, ErrorWrapper errors) {
		if(st.closedStatement instanceof ForSt || st.closedStatement instanceof RangedForSt)
			instructions.jmp(getLabel(st.closedStatement));
		else if(st.closedStatement instanceof IfSt && ((IfSt)st.closedStatement).elseStatement != null)
			instructions.jmp(getLabel(((IfSt)st.closedStatement).elseStatement.sectionEnd));
		else if(st.closedStatement instanceof WhileSt)
			instructions.jmp(getLabel(st.closedStatement));
		instructions.label(getLabel(st));
	}
	
	private void writeVarDeclaration(VariableDeclaration st, ErrorWrapper errors) {
		mem.writeDeclaration(st, errors);
	}
	
	private void writeIfStatement(IfSt st, ErrorWrapper errors) {
		opWriter.writeJump(st.getCondition(), getLabel(st.sectionEnd), errors);
	}
	
	private void writeElseStatement(ElseSt st, ErrorWrapper errors) {
		
	}
	
	private void writeWhileStatement(WhileSt st, ErrorWrapper errors) {
		instructions.label(getLabel(st));
		opWriter.writeJump(st.getCondition(), getLabel(st.sectionEnd), errors);
	}
	
	private void writeForStatement(ForSt st, ErrorWrapper errors) {
		writeForStatement(st, getLabel(st), errors);
	}
	
	private void writeRangedForStatement(RangedForSt st, ErrorWrapper errors) {
		MemAddress varAddress = mem.writeDeclaration(st.getVariableDeclaration(), errors);
		mem.declareDummyStackVariable("RangedFor.Step");
		mem.declareDummyStackVariable("RangedFor.Max");
		MemAddress stepAddress = varAddress.addOffset(-8);
		MemAddress maxAddress = varAddress.addOffset(-16);
		mem.writeTo(stepAddress, st.getStep(), errors);
		mem.writeTo(maxAddress, st.getMax(), errors);
		String label = getLabel(st);
		mem.getVarAddress(st.getVariableDeclaration().getPrototype());
		instructions.mov(Register.RAX, varAddress);
		instructions.jmp(label + "_firstpass");
		instructions.label(label);
		instructions.mov(Register.RAX, varAddress);
		instructions.add(OpCode.ADD, Register.RAX, stepAddress);
		instructions.mov(varAddress, Register.RAX);
		instructions.label(label + "_firstpass");
		instructions.mov(Register.RBX, maxAddress);
		instructions.cmp(Register.RAX, Register.RBX);
		instructions.add(OpCode.JGE, getLabel(st.sectionEnd));
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
			instructions.jmp(label + "_firstpass");
		instructions.label(label);
		if(st.affectation != null) {
			writeAffectationStatement(st.affectation, errors);
			instructions.label(label + "_firstpass");
		}
		if(st.getCondition() instanceof BoolLiteral && ((BoolLiteral) st.getCondition()).value) {
			instructions.jmp(label);
		} else {
			opWriter.writeJump(st.getCondition(), getLabel(st.sectionEnd), errors);
		}
	}
	
	private void writeReturnStatement(ReturnSt st, boolean writeJmp, ErrorWrapper errors) {
		expWriter.writeExpression(st.getExpression(), errors);
		if(writeJmp)
			instructions.jmp(".ret");
	}
	
	private void writeFunctionStatement(FunctionSt st, ErrorWrapper errors) {
		expWriter.writeExpression(st.getFunction(), errors);
	}
	
	private void writeAffectationStatement(AffectationSt st, ErrorWrapper errors) {
		mem.writeAffectationTo(st.getVariable(), st.getValue(), errors);
	}
	
	private void writeMultipleAffectationSt(MultipleAffectationSt st, ErrorWrapper errors) {
		mem.writeMultipleAffectationTo(st.getVariables(), st.getValues(), errors);
	}
	
	private void writeOperationStatement(OperationSt st, ErrorWrapper errors) {
		expWriter.writeExpression(st.getOperation(), errors);
	}
	
}
