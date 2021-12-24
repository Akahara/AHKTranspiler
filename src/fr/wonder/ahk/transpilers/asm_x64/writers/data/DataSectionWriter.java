package fr.wonder.ahk.transpilers.asm_x64.writers.data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.StrLiteral;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.prototypes.blueprints.BlueprintImplementation;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiled.units.sections.SimpleLambda;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.transpilers.asm_x64.writers.RegistryManager;
import fr.wonder.ahk.transpilers.asm_x64.writers.UnitWriter;
import fr.wonder.ahk.transpilers.common_x64.InstructionSet;
import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.declarations.EmptyLine;
import fr.wonder.ahk.transpilers.common_x64.declarations.ExternDeclaration;
import fr.wonder.ahk.transpilers.common_x64.declarations.GlobalDeclaration;
import fr.wonder.ahk.transpilers.common_x64.declarations.GlobalVarDeclaration;
import fr.wonder.ahk.transpilers.common_x64.declarations.Label;
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
			if(f.modifiers.visibility != DeclarationVisibility.GLOBAL || f.modifiers.hasModifier(Modifier.NATIVE))
				continue;
			instructions.add(new GlobalDeclaration(RegistryManager.getGlobalRegistry(f.getPrototype())));
			instructions.add(new GlobalDeclaration(RegistryManager.getClosureRegistry(f.getPrototype())));
			hasGlobal = true;
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

	public void writeLambdas() {
		if(unit.lambdas.isEmpty())
			return;
		instructions.comment("Lambdas");
		for(SimpleLambda lambda : unit.lambdas) {
			String closure = writer.registries.getLambdaClosureRegistry(lambda);
			String lambdaFunctionLabel = writer.registries.getLambdaRegistry(lambda);
			instructions.add(new GlobalVarDeclaration(closure, MemSize.QWORD, lambdaFunctionLabel));
		}
		instructions.skip();
	}
	
	public void writeBIPs() {
		if(!unitHasBIPs())
			return;
		instructions.comment("BIPs");
		for(StructSection struct : unit.structures) {
			for(BlueprintImplementation bp : struct.implementedBlueprints) {
				if(!isNonEmptyBIP(bp))
					continue;
				// the order in which values are declared depends on the blueprint layout
				instructions.add(new Label(RegistryManager.getStructBlueprintImplRegistry(bp)));
				for(FunctionPrototype fop : bp.functions)
					instructions.add(new GlobalVarDeclaration(MemSize.POINTER, RegistryManager.getFunctionRegistry(fop)));
				for(VariablePrototype vop : bp.variables)
					instructions.add(new GlobalVarDeclaration(MemSize.POINTER, 
							""+writer.types.getConcreteType(struct.getPrototype()).getOffset(vop.getName())));
				for(OverloadedOperatorPrototype oop : bp.operators)
					instructions.add(new GlobalVarDeclaration(MemSize.POINTER, RegistryManager.getFunctionRegistry(oop.function)));
			}
		}
		instructions.skip();
	}
	
	private boolean unitHasBIPs() {
		for(StructSection struct : unit.structures) {
			for(BlueprintImplementation bip : struct.implementedBlueprints) {
				if(isNonEmptyBIP(bip))
					return true;
			}
		}
		return false;
	}
	
	private static boolean isNonEmptyBIP(BlueprintImplementation bip) {
		return bip.functions.length > 0 || bip.operators.length > 0 || bip.variables.length > 0;
	}

	public void writeStrConstants(List<StrLiteral> strConstants) {
		if(strConstants.isEmpty())
			return;
		instructions.comment("String constants");
		for(StrLiteral cst : strConstants)
			instructions.add(new StringDefinition(writer.getStringConstantLabel(cst), cst.value));
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