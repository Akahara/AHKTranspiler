package fr.wonder.ahk.transpilers.asm_x64.writers;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.ExpressionHolder;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.StrLiteral;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.Prototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.handles.TranspilableHandle;
import fr.wonder.ahk.transpilers.asm_x64.ConcreteTypesTable;
import fr.wonder.ahk.transpilers.asm_x64.natives.operations.AsmOperationWriter;
import fr.wonder.ahk.transpilers.asm_x64.units.modifiers.NativeModifier;
import fr.wonder.ahk.transpilers.common_x64.GlobalLabels;
import fr.wonder.ahk.transpilers.common_x64.InstructionSet;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.addresses.ImmediateValue;
import fr.wonder.ahk.transpilers.common_x64.addresses.LabelAddress;
import fr.wonder.ahk.transpilers.common_x64.declarations.ExternDeclaration;
import fr.wonder.ahk.transpilers.common_x64.declarations.GlobalDeclaration;
import fr.wonder.ahk.transpilers.common_x64.declarations.GlobalVarDeclaration;
import fr.wonder.ahk.transpilers.common_x64.declarations.SectionDeclaration;
import fr.wonder.ahk.transpilers.common_x64.instructions.OpCode;
import fr.wonder.ahk.transpilers.common_x64.instructions.SpecialInstruction;
import fr.wonder.ahk.transpilers.common_x64.macros.StringDefinition;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class UnitWriter {
	
	public static InstructionSet writeUnit(TranspilableHandle handle, Unit unit, ErrorWrapper errors) {
		UnitWriter uw = new UnitWriter(handle, unit);
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
		
		return uw.instructions;
	}
	
	public final InstructionSet instructions = new InstructionSet();
	
	public final ConcreteTypesTable types = new ConcreteTypesTable();
	public final TranspilableHandle projectHandle;
	public final Unit unit;
	public final MemoryManager mem;
	public final ExpressionWriter expWriter;
	public final AsmOperationWriter asmWriter;
	/** populated by {@link #writeDataSegment(ErrorWrapper)} and used by {@link #getLabel(StrLiteral)} */
	private final List<StrLiteral> strConstants = new ArrayList<>();
	
	private int specialCallCount = 0;
	
	private UnitWriter(TranspilableHandle handle, Unit unit) {
		this.projectHandle = handle;
		this.unit = unit;
		this.mem = new MemoryManager(this);
		this.expWriter = new ExpressionWriter(this);
		this.asmWriter = new AsmOperationWriter(this);
	}
	
	// ------------------------ registries & labels ------------------------
	
	String getRegistry(Prototype<?> var) {
		if(var.getSignature().declaringUnit.equals(unit.fullBase))
			return getLocalRegistry(var);
		else
			return getGlobalRegistry(var);
	}
	
	public static String getUnitRegistry(Unit unit) {
		return getUnitRegistry(unit.fullBase);
	}
	
	public static String getUnitRegistry(String unitFullBase) {
		return unitFullBase.replaceAll("\\.", "_");
	}
	
	public static String getGlobalRegistry(Prototype<?> var) {
		if(var.getModifiers().hasModifier(Modifier.NATIVE))
			return var.getModifiers().getModifier(NativeModifier.class).nativeRef;
		
		return getUnitRegistry(var.getSignature().declaringUnit) + "_" + getLocalRegistry(var);
	}
	
	public static String getLocalRegistry(Prototype<?> var) {
		if(var.getModifiers().hasModifier(Modifier.NATIVE))
			return getGlobalRegistry(var);
		
		if(var instanceof VariablePrototype)
			return ((VariablePrototype) var).getName();
		if(var instanceof FunctionPrototype) {
			FunctionPrototype f = (FunctionPrototype) var;
			return f.getName() + "_" + f.getType().getSignature();
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
		instructions.add(new SpecialInstruction("%include\"intrinsic.asm\""));
		instructions.section(SectionDeclaration.DATA);
		instructions.skip();
		
		instructions.add(new GlobalDeclaration(getUnitRegistry(unit) + "_init"));
		for(VariableDeclaration v : unit.variables) {
			if(v.getVisibility() == DeclarationVisibility.GLOBAL && !v.getModifiers().hasModifier(Modifier.NATIVE))
				instructions.add(new GlobalDeclaration(getGlobalRegistry(v.getPrototype())));
		}
		if(unit.variables.length != 0)
			instructions.skip();
		for(FunctionSection f : unit.functions) {
			if(f.getVisibility() == DeclarationVisibility.GLOBAL && !f.getModifiers().hasModifier(Modifier.NATIVE))
				instructions.add(new GlobalDeclaration(getGlobalRegistry(f.getPrototype())));
		}
		if(unit.functions.length != 0)
			instructions.skip();
		instructions.add(new ExternDeclaration(GlobalLabels.GLOBAL_FLOATST));
		instructions.add(new ExternDeclaration(GlobalLabels.SPECIAL_ALLOC));
		instructions.add(new ExternDeclaration(GlobalLabels.SPECIAL_THROW));
		for(VarAccess i : unit.prototype.externalAccesses) {
			// i can safely be casted to a prototype because it cannot be a function
			// argument. It's either a Variable or a Function prototype.
			instructions.add(new ExternDeclaration(getGlobalRegistry((Prototype<?>) i)));
		}
		if(unit.importations.length != 0)
			instructions.skip();
		boolean hasNativeRefs = false;
		for(FunctionSection f : unit.functions) {
			if(f.modifiers.hasModifier(Modifier.NATIVE)) {
				instructions.add(new ExternDeclaration(f.modifiers.getModifier(NativeModifier.class).nativeRef));
				hasNativeRefs = true;
			}
		}
		if(hasNativeRefs)
			instructions.skip();
		
		collectStrConstants(strConstants, unit.variables);
		for(FunctionSection f : unit.functions)
			collectStrConstants(strConstants, f.body);
		for(StrLiteral cst : strConstants)
			instructions.add(new StringDefinition(getLabel(cst), cst.value));
		if(!strConstants.isEmpty())
			instructions.skip();
		
		for(VariableDeclaration var : unit.variables) {
			if(var.getVisibility() == DeclarationVisibility.GLOBAL)
				instructions.label(getGlobalRegistry(var.getPrototype()));
			String value;
			if(initializedVariables.contains(var))
				value = mem.getValueString((LiteralExp<?>) var.getDefaultValue());
			else
				value = "0";
			instructions.add(new GlobalVarDeclaration(getLocalRegistry(var.getPrototype()), MemSize.QWORD, value));
		}
		if(unit.variables.length != 0)
			instructions.skip();
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
	
	// *** Text segment ***
	
	private void writeTextSegment(List<VariableDeclaration> initializableVariables, ErrorWrapper errors) {
		instructions.section(SectionDeclaration.TEXT);
		instructions.skip();
		
		FunctionSection initFunction = new FunctionSection(Invalids.SOURCE, 0, 0, 0);
		
		// write the initialization function
		instructions.label(getUnitRegistry(unit) + "_init");
		if(!initializableVariables.isEmpty()) {
			instructions.createScope();
			mem.enterFunction(initFunction, 0);
			for(VariableDeclaration var : initializableVariables) {
				Expression defaultVal = var.getDefaultValue();
				if(defaultVal == null)
					defaultVal = new NoneExp(MemSize.getPointerSize(var.getType()).bytes);
				mem.writeTo(new LabelAddress(getRegistry(var.getPrototype())), defaultVal, errors);
			}
			instructions.endScope();
		}
		instructions.ret();
		instructions.skip();
		
		for(FunctionSection func : unit.functions) {
			if(func.modifiers.hasModifier(Modifier.NATIVE))
				continue;
			
			FunctionWriter funcWriter = new FunctionWriter(this, func);
			
			if(func.getVisibility() == DeclarationVisibility.GLOBAL)
				instructions.label(getGlobalRegistry(func.getPrototype()));
			instructions.label(getLocalRegistry(func.getPrototype()));
			funcWriter.writeFunction(func, errors);
			instructions.skip();
		}
	}
	
	// --------------------------- special calls ---------------------------
	
	public String getSpecialLabel() {
		return ".special_" + (specialCallCount++);
	}
	
	public void testThrowError() {
		String label = getSpecialLabel();
		instructions.test(Register.RAX, Register.RAX);
		instructions.add(OpCode.JNS, new LabelAddress(label));
		instructions.call(GlobalLabels.SPECIAL_THROW);
		instructions.label(label);
	}
	
	public void callAlloc(int size) {
		instructions.push(new ImmediateValue(size), MemSize.QWORD);
		instructions.call(GlobalLabels.SPECIAL_ALLOC);
		// TODO0 implement other calling conventions (__stdcall currently)
		testThrowError();
	}
	
}
