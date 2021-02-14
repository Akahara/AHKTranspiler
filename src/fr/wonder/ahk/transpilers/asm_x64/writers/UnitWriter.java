package fr.wonder.ahk.transpilers.asm_x64.writers;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.ExpressionHolder;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.StrLiteral;
import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.UnitImportation;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.handles.TranspilableHandle;
import fr.wonder.ahk.handles.CompiledHandle;
import fr.wonder.ahk.transpilers.asm_x64.natives.operations.AsmWriter;
import fr.wonder.ahk.transpilers.asm_x64.units.modifiers.NativeModifier;
import fr.wonder.ahk.transpilers.asm_x64.writers.memory.MemoryManager;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class UnitWriter {
	
	public static void writeUnit(TranspilableHandle handle, Unit unit, TextBuffer tb, ErrorWrapper errors) {
		UnitWriter uw = new UnitWriter(handle, unit, tb);
		// variables that must be initialized (because they are not literals or computable constants)
		List<VariableDeclaration> initializableVariables = new ArrayList<>();
		// all other variables (which values can be computed beforehand)
		List<VariableDeclaration> initializedVariables = new ArrayList<>();
		
		// TODO use the compiler to optimize directly computable values
		for(VariableDeclaration var : unit.variables) {
			if(var.getDefaultValue() instanceof LiteralExp<?>)
				initializedVariables.add(var);
			else
				initializableVariables.add(var);
		}
		
		uw.writeDataSegment(initializedVariables, errors);
		uw.writeTextSegment(initializableVariables, errors);
	}
	
	public final TranspilableHandle projectHandle;
	public final Unit unit;
	public final TextBuffer buffer;
	public final MemoryManager mem;
	public final ExpressionWriter expWriter;
	public final AsmWriter asmWriter;
	/** populated by {@link #writeDataSegment(ErrorWrapper)} and used by {@link #getLabel(StrLiteral)} */
	private final List<StrLiteral> strConstants = new ArrayList<>();
	
	private int specialCallCount = 0;
	
	private UnitWriter(TranspilableHandle handle, Unit unit, TextBuffer tb) {
		this.projectHandle = handle;
		this.unit = unit;
		this.buffer = tb;
		this.mem = new MemoryManager(this);
		this.expWriter = new ExpressionWriter(this);
		this.asmWriter = new AsmWriter(this);
	}
	
	// ------------------------ registries & labels ------------------------
	
	String getRegistry(ValueDeclaration var) {
		if(var.getUnit() == unit)
			return getLocalRegistry(var);
		else
			return getGlobalRegistry(var);
	}
	
	public static String getUnitRegistry(Unit unit) {
		return unit.getFullBase().replaceAll("\\.", "_");
	}
	
	public static String getGlobalRegistry(ValueDeclaration var) {
		if(var.getModifiers().hasModifier(Modifier.NATIVE))
			return var.getModifiers().getModifier(NativeModifier.class).nativeRef;
		
		return getUnitRegistry(var.getUnit()) + "_" + getLocalRegistry(var);
	}
	
	public static String getLocalRegistry(ValueDeclaration var) {
		if(var.getModifiers().hasModifier(Modifier.NATIVE))
			return getGlobalRegistry(var);
		
		if(var instanceof VariableDeclaration)
			return var.getName();
		if(var instanceof FunctionSection) {
			FunctionSection f = (FunctionSection) var;
			return var.getName() + "_" + f.getFunctionType().getSignature();
		}
		
		throw new IllegalArgumentException("Unimplemented registry " + var.getClass());
	}
	
	public String getLabel(StrLiteral lit) {
		// beware! strConstants.indexOf cannot be used because the #equals method of StrLiteral
		// will return true if two literals hold equal strings.
		for(int i = 0; i < strConstants.size(); i++) {
			if(strConstants.get(i) == lit)
				return "str_cst_" + i;
		}
		throw new IllegalStateException("Unregistered string constant");
	}

	// ------------------------------ segments -----------------------------
	
	// *** Data segment ***
	
	private void writeDataSegment(List<VariableDeclaration> initializedVariables, ErrorWrapper errors) {
		buffer.appendLine("%include\"intrinsic.asm\"");
		buffer.appendLine("section .data");
		buffer.appendLine();
		
		buffer.appendLine("global " + getUnitRegistry(unit) + "_init");
		writeGlobalStatements(unit.variables);
		writeGlobalStatements(unit.functions);
		buffer.appendLine("extern "+GLOBAL_FLOATST);
		buffer.appendLine("extern "+SPECIAL_ALLOC);
		buffer.appendLine("extern "+SPECIAL_THROW);
		for(UnitImportation i : unit.importations)
			writeExternStatements(i.unit);
		if(unit.importations.length != 0)
			buffer.appendLine();
		
		collectStrConstants(strConstants, unit.variables);
		for(FunctionSection f : unit.functions)
			collectStrConstants(strConstants, f.body);
		for(StrLiteral cst : strConstants)
			buffer.appendLine("def_string " + getLabel(cst) + ",`" + cst.value + "`");
		if(!strConstants.isEmpty())
			buffer.appendLine();
		
		for(VariableDeclaration var : unit.variables) {
			if(var.getVisibility() == DeclarationVisibility.GLOBAL)
				buffer.appendLine(getGlobalRegistry(var) + ":");
			String value;
			if(initializedVariables.contains(var))
				value = mem.getValueString((LiteralExp<?>) var.getDefaultValue());
			else
				value = "0";
			buffer.appendLine(getLocalRegistry(var) + " dq " + value);
		}
		if(unit.variables.length != 0)
			buffer.appendLine();
		
	}
	
	private static void collectStrConstants(List<StrLiteral> csts, ExpressionHolder[] vars) {
		for(ExpressionHolder h : vars) {
			for(Expression e : h.getExpressions()) {
				if(e instanceof StrLiteral)
					csts.add((StrLiteral) e);
				else
					collectStrConstants(csts, h.getExpressions());
			}
		}
	}
	
	private <T extends ValueDeclaration> void writeGlobalStatements(T[] vars) {
		for(ValueDeclaration v : vars) {
			if(v.getVisibility() == DeclarationVisibility.GLOBAL && !v.getModifiers().hasModifier(Modifier.NATIVE))
				buffer.appendLine("global " + getGlobalRegistry(v));
		}
		if(vars.length != 0)
			buffer.appendLine();
	}
	
	private void writeExternStatements(Unit imported) {
		ValueDeclaration[] externFields = projectHandle.externFields.get(imported);
		for(ValueDeclaration v : externFields)
			buffer.appendLine("extern " + getGlobalRegistry(v));
	}
	
	// *** Text segment ***
	
	private void writeTextSegment(List<VariableDeclaration> initializableVariables, ErrorWrapper errors) {
		buffer.appendLine("section .text");
		buffer.appendLine();
		
		FunctionSection initFunction = new FunctionSection(unit);
		
		// write the initialization function
		buffer.appendLine(getUnitRegistry(unit) + "_init:");
		if(!initializableVariables.isEmpty()) {
			buffer.writeLine("push rbp");
			buffer.writeLine("mov rbp,rsp");
			mem.enterFunction(initFunction);
			for(VariableDeclaration var : initializableVariables) {
				Expression defaultVal = var.getDefaultValue();
				if(defaultVal == null)
					defaultVal = new NoneExp(var.getType().getSize());
				mem.writeTo(var, defaultVal, errors);
			}
			buffer.writeLine("mov rsp,rbp");
			buffer.writeLine("pop rbp");
		}
		buffer.writeLine("ret");
		buffer.appendLine();
		
		for(FunctionSection func : unit.functions) {
			if(func.modifiers.hasModifier(Modifier.NATIVE))
				continue;
			
			FunctionWriter funcWriter = new FunctionWriter(this, func);
			
			if(func.getVisibility() == DeclarationVisibility.GLOBAL)
				buffer.appendLine(getGlobalRegistry(func) + ":");
			buffer.appendLine(getLocalRegistry(func) + ":");
			funcWriter.writeFunction(func, errors);
			buffer.appendLine();
		}
	}
	
	// --------------------------- special calls ---------------------------
	
	public static final String GLOBAL_FLOATST = "floatst";
	public static final String SPECIAL_ALLOC = "mem_alloc_block";
	public static final String SPECIAL_THROW = "e_throw";

	public String getSpecialLabel() {
		return ".special_" + (specialCallCount++);
	}
	
	public void testThrowError() {
		String label = getSpecialLabel();
		buffer.writeLine("test rax,rax");
		buffer.writeLine("jns " + label);
		buffer.writeLine("call e_throw");
		buffer.appendLine(label + ":");
	}
	
	public void callAlloc(int size) {
		buffer.writeLine("push qword " + size);
		buffer.writeLine("call " + SPECIAL_ALLOC);
		// TODO0 implement other calling conventions (__stdcall currently)
		testThrowError();
	}
	
}
