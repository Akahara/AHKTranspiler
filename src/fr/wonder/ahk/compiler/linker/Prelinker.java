package fr.wonder.ahk.compiler.linker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.sections.ConstructorDefaultValue;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.StructConstructor;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.types.FunctionArguments;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.types.Triplet;
import fr.wonder.commons.utils.ArrayOperator;

class Prelinker {

	static void computeSignaturesAndPrototypes(Unit unit) {
		for(FunctionSection func : unit.functions)
			func.setSignature(Signatures.of(func));
		for(VariableDeclaration var : unit.variables)
			var.setSignature(Signatures.of(var));
		for(StructSection struct : unit.structures) {
			for(VariableDeclaration mem : struct.members)
				mem.setSignature(Signatures.of(mem, struct));
			for(StructConstructor con : struct.constructors)
				con.setSignature(Signatures.of(con));
			struct.setSignature(Signatures.of(struct));
		}
		
		FunctionPrototype[] functions = ArrayOperator.map(
				unit.functions,
				FunctionPrototype[]::new,
				FunctionSection::getPrototype);
		VariablePrototype[] variables = ArrayOperator.map(
				unit.variables,
				VariablePrototype[]::new,
				VariableDeclaration::getPrototype);
		StructPrototype[] structures = ArrayOperator.map(
				unit.structures,
				StructPrototype[]::new,
				StructSection::getPrototype);
		unit.prototype = new UnitPrototype(
				unit.fullBase,
				unit.importations,
				functions,
				variables,
				structures);
	}
	
	/** Computes functions and variables signatures, validates units and sets unit.prototype */
	static void prelinkUnit(Unit unit, UnitPrototype[] units, ErrorWrapper errors) {
		Map<String, StructPrototype[]> declaredStructures = new HashMap<>();
		for(UnitPrototype u : units)
			declaredStructures.put(u.fullBase, u.structures);
		
		for(UnitPrototype u : unit.prototype.filterImportedUnits(units)) {
			for(StructPrototype s : u.structures) {
				if(s.modifiers.visibility == DeclarationVisibility.GLOBAL)
					unit.prototype.externalAccesses.add(s);
			}
		}
		
		// link the structure types instances to their structure prototypes
		
		for(Triplet<VarStructType, Token, Integer> composite : unit.usedStructTypes.values()) {
			VarStructType structType = composite.a;
			structType.structure = searchStructSection(unit, structType.name, declaredStructures, errors);
			if(structType.structure == Invalids.STRUCT_PROTOTYPE)
				errors.add("Unknown structure type used: " + structType.name + 
						" (" + composite.c + " references)" + composite.b.getErr());
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
			
			// check function duplicates
			for(int j = 0; j < i; j++) {
				if(unit.functions[j].name.equals(func.name)) {
					errors.add("Two functions have the same name: " + func.name+
							func.getErr() + unit.functions[j].getErr());
				}
			}
			
			// check duplicate names in arguments
			for(int j = 1; j < func.arguments.length; j++) {
				FunctionArgument arg1 = func.arguments[j];
				for(int k = 0; k < j; k++) {
					FunctionArgument arg2 = func.arguments[k];
					if(arg1.name.equals(arg2.name))
						errors.add("Two arguments have the same name:" + arg1.getErr() + arg2.getErr());
				}
			}
		}
		
		// check structures
		for(int i = 0; i < unit.structures.length; i++) {
			StructSection structure = unit.structures[i];
			// check structure members duplicates
			for(int j = 1; j < structure.members.length; j++) {
				VariableDeclaration v1 = structure.members[j];
				for(int k = 0; k < j; k++) {
					VariableDeclaration v2 = structure.members[k];
					if(v1.name.equals(v2.name))
						errors.add("Two struct members have the same name:" + v1.getErr() + v2.getErr());
				}
			}
			
			// check constructors
			for(int j = 1; j < structure.constructors.length; j++) {
				StructConstructor c1 = structure.constructors[i];
				
				// check constructor argument duplicates and mismatch
				for(int k = 0; k < c1.arguments.length; k++) {
					FunctionArgument arg1 = c1.arguments[k];
					VariableDeclaration member = structure.getMember(arg1.name);
					if(member == null) {
						errors.add("Constructor argument " + arg1.name + " does not refer to a "
								+ "member of this structure:" + arg1.getErr());
					} else if(!member.getType().equals(arg1.type)) {
						errors.add("Constructor argument " + arg1.name + " does not match type "
								+ member.getType() + " of structure member:" + arg1.getErr() + member.getErr());
					}
					for(int l = 0; l < k; l++) {
						FunctionArgument arg2 = c1.arguments[l];
						if(arg1.name.equals(arg2.name))
							errors.add("Duplicate constructor argument name:" + arg1.getErr() + arg2.getErr());
					}
				}
				
				for(int k = 0; k < j; k++) {
					StructConstructor c2 = structure.constructors[j];
					if(FunctionArguments.matchNoConversions(c1.getArgumentTypes(), c2.getArgumentTypes()))
						errors.add("Duplicate constructor found:" + c1.getErr() + c2.getErr());
				}
			}
			
			// check for null instance
			for(int j = 0; j < structure.nullFields.length; j++) {
				ConstructorDefaultValue f1 = structure.nullFields[j];
				VariableDeclaration member = structure.getMember(f1.name);
				if(member == null)
					errors.add("Null field " + f1.name + " does not refer to a member of"
							+ " this structure:" + f1.getErr());
				
				for(int k = 0; k < j; k++) {
					ConstructorDefaultValue f2 = structure.nullFields[k];
					if(f1.name.equals(f2.name))
						errors.add("Duplicate null field found:" + f1.getErr() + f2.getErr());
				}
			}
		}
		
		List<StructPrototype> accessibleStructs = new ArrayList<>();
		accessibleStructs.addAll(Arrays.asList(declaredStructures.get(unit.fullBase)));
		for(String importation : unit.importations)
			accessibleStructs.addAll(Arrays.asList(declaredStructures.get(importation)));
		
		for(int i = 1; i < accessibleStructs.size(); i++) {
			StructPrototype s1 = accessibleStructs.get(i);
			for(int j = 0; j < i; j++) {
				StructPrototype s2 = accessibleStructs.get(j);
				if(s1.getName().equals(s2.getName()))
					errors.add("Two accessible structs have the same name: "
							+ s1.getSignature().declaringUnit+'@'+s1.getName() + " and "
							+ s2.getSignature().declaringUnit+'@'+s2.getName());
			}
		}
	}

	private static StructPrototype searchStructSection(Unit unit, String name,
			Map<String, StructPrototype[]> declaredStructures, ErrorWrapper errors) {
		for(StructSection structure : unit.structures) {
			if(structure.name.equals(name))
				return structure.getPrototype();
		}
		for(String imported : unit.importations) {
			StructPrototype[] structures = declaredStructures.get(imported);
			for(StructPrototype structure : structures) {
				if(structure.getName().equals(name) && (
						structure.signature.declaringUnit.equals(unit.fullBase) ||
						structure.modifiers.visibility == DeclarationVisibility.GLOBAL))
					return structure;
			}
		}
		return Invalids.STRUCT_PROTOTYPE;
	}
	
}
