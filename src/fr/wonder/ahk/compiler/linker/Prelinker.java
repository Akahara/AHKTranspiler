package fr.wonder.ahk.compiler.linker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.UnitCompilationState;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.types.Triplet;

public class Prelinker {

	/** Computes functions and variables signatures, validates units and sets unit.prototype */
	public static void prelinkUnit(Unit unit, Map<String, StructSection[]> declaredStructures,
			ErrorWrapper errors) {
		
		if(unit.compilationState != UnitCompilationState.PARSED)
			throw new IllegalStateException("Cannot prelink an unit with state " + unit.compilationState);
		
		unit.compilationState = UnitCompilationState.PRELINKED_WITH_ERRORS;
		
		for(Triplet<VarStructType, Token, Integer> composite : unit.usedStructTypes) {
			VarStructType structType = composite.a;
			structType.structure = searchStructSection(unit, structType.name, declaredStructures, errors);
			if(structType.structure == Invalids.STRUCT)
				errors.add("Unknown structure type used: " + structType.name + 
						" (" + composite.c + " references)" + composite.b.getErr());
		}
		
		for(FunctionSection func : unit.functions) {
			func.setSignature(new Signature(
					unit.fullBase,
					func.name,
					func.name + "_" + func.getFunctionType().getSignature()));
		}
		for(VariableDeclaration var : unit.variables) {
			var.setSignature(new Signature(
					unit.fullBase,
					var.name,
					var.name));
		}
		
		// validate unit
		
		for(int i = 0; i < unit.variables.length; i++) {
			String varName = unit.variables[i].name;
			// check variable duplicates
			for(int j = 0; j < i; j++) {
				if(unit.variables[j].name.equals(varName)) {
					errors.add("Two variables have the same name: " + varName +
							unit.variables[j].getErr() + unit.variables[i].getErr());
				}
			}
		}
		
		for(int i = 0; i < unit.functions.length; i++) {
			FunctionSection func = unit.functions[i];
			// check variable name conflicts
			for(VariableDeclaration var : unit.variables) {
				if(var.name.equals(func.name))
					errors.add("A function has the same name as a variable: " +
							var.name + func.getErr() + var.getErr());
			}
			
			// check signatures duplicates
			String funcSig = func.getSignature().computedSignature;
			for(int j = 0; j < i; j++) {
				if(unit.functions[j].getSignature().computedSignature.equals(funcSig)) {
					errors.add("Two functions have the same signature: " + funcSig +
							unit.functions[j].getErr() + unit.functions[i].getErr());
				}
			}
			
			// check duplicate names in arguments
			for(int j = 1; j < func.arguments.length; j++) {
				for(int k = 0; k < j; k++) {
					if(func.arguments[j].name.equals(func.arguments[k].name))
						errors.add("Two arguments have the same name:" + func.getErr());
				}
			}
		}
		
		for(int i = 0; i < unit.structures.length; i++) {
			StructSection structure = unit.structures[i];
			for(int j = 1; j < structure.members.length; j++) {
				for(int k = 0; k < j; k++) {
					if(structure.members[j].name.equals(structure.members[k].name))
						errors.add("Two struct members have the same name:" + structure.members[j].getErr());
				}
			}
		}
		
		List<StructSection> accessibleStructs = new ArrayList<>();
		accessibleStructs.addAll(Arrays.asList(unit.structures));
		for(String importation : unit.importations)
			accessibleStructs.addAll(Arrays.asList(declaredStructures.get(importation)));
		
		for(int i = 1; i < accessibleStructs.size(); i++) {
			StructSection s1 = accessibleStructs.get(i);
			for(int j = 0; j < i; j++) {
				StructSection s2 = accessibleStructs.get(j);
				if(s1.structName.equals(s2.structName))
					errors.add("Two accessible structs have the same name:" + s1.getErr() + s2.getErr());
			}
		}
		
		Prototypes.buildPrototype(unit);
		
		if(errors.noErrors())
			unit.compilationState = UnitCompilationState.PRELINKED;
	}

	private static StructSection searchStructSection(Unit unit, String name,
			Map<String, StructSection[]> declaredStructures, ErrorWrapper errors) {
		for(StructSection structure : unit.structures) {
			if(structure.structName.equals(name))
				return structure;
		}
		for(String imported : unit.importations) {
			StructSection[] structures = declaredStructures.get(imported);
			for(StructSection structure : structures) {
				if(structure.structName.equals(name))
					return structure;
			}
		}
		return Invalids.STRUCT;
	}
	
}
