package fr.wonder.ahk.transpilers.asm_x64.writers.data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.transpilers.asm_x64.units.modifiers.NativeModifier;
import fr.wonder.ahk.transpilers.asm_x64.writers.RegistryManager;
import fr.wonder.ahk.transpilers.asm_x64.writers.UnitWriter;
import fr.wonder.ahk.transpilers.common_x64.InstructionSet;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.declarations.EmptyLine;
import fr.wonder.ahk.transpilers.common_x64.declarations.ExternDeclaration;
import fr.wonder.ahk.transpilers.common_x64.declarations.GlobalDeclaration;
import fr.wonder.ahk.transpilers.common_x64.declarations.GlobalVarDeclaration;
import fr.wonder.ahk.transpilers.common_x64.instructions.Instruction;
import fr.wonder.ahk.transpilers.common_x64.macros.StringDefinition;

public class DataSectionWriter {
	
	private final UnitWriter writer;
	private final Unit unit;
	private final InstructionSet instructions;
	
	private final Set<String> requiredExternDeclarations = new HashSet<>();
	private int externDeclarationsIndex;
	
	public DataSectionWriter(UnitWriter writer) {
		this.writer = writer;
		this.unit = writer.unit;
		this.instructions = writer.instructions;
	}
	
	public final void addExternDeclaration(String external) {
		requiredExternDeclarations.add(external);
	}

	public void writeGlobalDeclarations() {
		instructions.add(new GlobalDeclaration(RegistryManager.getUnitInitFunctionRegistry(unit)));
		
		boolean hasGlobal;
		
		hasGlobal = false;
		for(VariableDeclaration v : unit.variables) {
			if(v.modifiers.visibility != DeclarationVisibility.GLOBAL || v.modifiers.hasModifier(Modifier.NATIVE))
				continue;
			instructions.add(new GlobalDeclaration(RegistryManager.getGlobalRegistry(v.getPrototype())));
			hasGlobal = true;
		}
		if(hasGlobal)
			instructions.skip();
		
		hasGlobal = false;
		for(FunctionSection f : unit.functions) {
			if(f.modifiers.visibility == DeclarationVisibility.GLOBAL) {
				instructions.add(new GlobalDeclaration(RegistryManager.getGlobalRegistry(f.getPrototype())));
				instructions.add(new GlobalDeclaration(RegistryManager.getClosureRegistry(f.getPrototype())));
				hasGlobal = true;
			}
			if(f.modifiers.hasModifier(Modifier.NATIVE)) {
				instructions.add(new ExternDeclaration(f.modifiers.getModifier(NativeModifier.class).nativeRef));
				hasGlobal = true;
			}
		}
		if(hasGlobal)
			instructions.skip();
		
		hasGlobal = false;
		for(StructSection s : unit.structures) {
			if(s.modifiers.visibility != DeclarationVisibility.GLOBAL)
				continue;
			instructions.add(new GlobalDeclaration(writer.registries.getStructNullRegistry(s.getPrototype())));
			hasGlobal = true;
		}
		if(hasGlobal)
			instructions.skip();

		
		// keep track of where to insert the 'extern' declarations in the
		// generated assembly file. They cannot be known before the text
		// segment was written
		this.externDeclarationsIndex = instructions.instructions.size();
	}

	public void writeVariableDeclarations(List<VariableDeclaration> initializedVariables) {
		if(unit.variables.length == 0)
			return;
		instructions.comment("Global variables");
		for(VariableDeclaration var : unit.variables) {
			String value;
			if(initializedVariables.contains(var))
				value = writer.getValueString((LiteralExp<?>) var.getDefaultValue());
			else
				value = "0";
			String label = RegistryManager.getGlobalRegistry(var.getPrototype());
			instructions.add(new GlobalVarDeclaration(label, MemSize.QWORD, value));
		}
		instructions.skip();
	}

	public void writeFunctionClosures() {
		if(unit.functions.length == 0)
			return;
		instructions.comment("Functions closures");
		for(FunctionSection func : unit.functions) {
			String closure = RegistryManager.getClosureRegistry(func.getPrototype());
			String address = RegistryManager.getGlobalRegistry(func.getPrototype());
			instructions.add(new GlobalVarDeclaration(closure, MemSize.QWORD, address));
		}
		instructions.skip();
	}

	public void writeStrConstants(List<String> strConstants) {
		if(strConstants.isEmpty())
			return;
		instructions.comment("String constants");
		for(String cst : strConstants)
			instructions.add(new StringDefinition(writer.getStringConstantLabel(cst), cst));
		instructions.skip();
	}
	
	public void insertExternDeclarations() {
		if(requiredExternDeclarations.isEmpty())
			return;
		
		List<Instruction> externDeclarations =
				requiredExternDeclarations.stream()
				.sorted()
				.map(ExternDeclaration::new)
				.collect(Collectors.toList());
		externDeclarations.add(EmptyLine.EMPTY_LINE);
		externDeclarations.add(EmptyLine.EMPTY_LINE);
		instructions.addAll(externDeclarationsIndex, externDeclarations);
	}
	
}
