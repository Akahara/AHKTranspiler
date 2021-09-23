package fr.wonder.ahk.compiler.linker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.types.VarBoundStructType;
import fr.wonder.ahk.compiled.expressions.types.VarSelfType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.ExternalTypeAccess;
import fr.wonder.ahk.compiled.units.ExternalTypeAccess.ParametrizedAccess;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.BlueprintPrototype;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.TypeAccess;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.sections.Blueprint;
import fr.wonder.ahk.compiled.units.sections.BlueprintOperator;
import fr.wonder.ahk.compiled.units.sections.BlueprintRef;
import fr.wonder.ahk.compiled.units.sections.ConstructorDefaultValue;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.OverloadedOperator;
import fr.wonder.ahk.compiled.units.sections.StructConstructor;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.types.FunctionArguments;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.utils.ArrayOperator;

class Prelinker {

	private final Linker linker;
	
	Prelinker(Linker linker) {
		this.linker = linker;
	}
	
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
		for(Blueprint bp : unit.blueprints) {
			for(VariableDeclaration var : bp.variables)
				var.setSignature(Signatures.of(var, bp));
			for(FunctionSection func : bp.functions)
				func.setSignature(Signatures.of(func, bp));
			for(BlueprintOperator op : bp.operators)
				op.setSignature(Signatures.of(op, bp));
			bp.setSignature(Signatures.of(bp));
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
		BlueprintPrototype[] blueprints = ArrayOperator.map(
				unit.blueprints,
				BlueprintPrototype[]::new,
				Blueprint::getPrototype);
		unit.prototype = new UnitPrototype(
				unit.fullBase,
				unit.importations,
				functions,
				variables,
				structures,
				blueprints);
	}
	
	void prelinkTypes(Unit unit, ErrorWrapper errors) {
		// link the structure types instances to their structure prototypes
		linkStructTypes(unit, errors);
		linkBlueprints(unit, errors);
	}

	private void linkStructTypes(Unit unit, ErrorWrapper errors) {
		
		for(ExternalTypeAccess<VarStructType> structAccess : unit.usedStructTypes.getAccesses()) {
			VarStructType structType = structAccess.typeInstance;
			StructPrototype structure = linker.searchStructSection(unit, structType.name);
			if(structure == null) {
				errors.add("Unknown structure type used: " + structType.name + 
						" (" + structAccess.occurrenceCount + " references)" +
						structAccess.firstOccurrence.getErr());
				structure = Invalids.STRUCT_PROTOTYPE;
			} else {
				unit.prototype.externalAccesses.add(structure);
			}
			structType.structure = structure;
		}
	}
	
	private void linkBlueprints(Unit unit, ErrorWrapper errors) {
		
		for(ExternalTypeAccess<BlueprintRef> bpAccess : unit.usedBlueprintTypes.getAccesses()) {
			BlueprintRef refInstance = bpAccess.typeInstance;
			BlueprintPrototype blueprint = linker.searchBlueprint(unit, refInstance.name);
			if(blueprint == null) {
				errors.add("Unknown blueprint used: " + refInstance.name + 
						" (" + bpAccess.occurrenceCount + " references)" +
						bpAccess.firstOccurrence.getErr());
				blueprint = Invalids.BLUEPRINT_PROTOTYPE;
			} else {
				unit.prototype.externalAccesses.add(blueprint);
			}
			refInstance.blueprint = blueprint;
		}
	}
	
	/** Computes functions and variables signatures, validates units and sets unit.prototype */
	void prelinkUnit(Unit unit, ErrorWrapper errors) {
		
		linkParametrizedTypes(unit, errors);
		
		// validate unit
		
		validateVariableSet(unit.variables, errors);
		validateFunctionSet(unit.functions, unit.variables, errors);
		
		// check structures
		for(int i = 0; i < unit.structures.length; i++) {
			StructSection structure = unit.structures[i];
			validateVariableSet(structure.members, errors);
			validateStructConstructors(structure, errors);
			validateStructNull(structure, errors);
			validateStructOperators(structure, errors);
		}
		
		// check blueprints
		for(int i = 0; i < unit.blueprints.length; i++) {
			Blueprint blueprint = unit.blueprints[i];
			validateVariableSet(blueprint.variables, errors);
//			validateBlueprintConstructors(blueprint, errors);
			validateBlueprintOperators(blueprint, errors);
			validateFunctionSet(blueprint.functions, blueprint.variables, errors);
		}
		
		// make sure there cannot be confusion in types usage
		validateAccessibleStructures(unit, errors);
		validateAccessibleBlueprints(unit, errors);
	}

	private void linkParametrizedTypes(Unit unit, ErrorWrapper errors) {
		
		for(ExternalTypeAccess<VarStructType> structAccess : unit.usedStructTypes.getAccesses()) {
			StructPrototype structure = structAccess.typeInstance.structure;
			for(ParametrizedAccess parametrizedInstance : structAccess.parametrizedInstances) {
				VarBoundStructType boundType = parametrizedInstance.type;
				if(structure == Invalids.STRUCT_PROTOTYPE) {
					boundType.structure = Invalids.STRUCT_PROTOTYPE;
				} else if(GenericBindings.validateBindings(structure.genericContext, // FIX
						boundType.boundTypes, parametrizedInstance.occurrence, errors)) {
					boundType.structure = linker.typesTable.genericBindings.bindGenerics(
							structure, boundType.boundTypes, parametrizedInstance.genericContext);
				} else {
					boundType.structure = Invalids.STRUCT_PROTOTYPE;
				}
			}
		}
	}
	
	private static void validateVariableSet(VariableDeclaration[] variables, ErrorWrapper errors) {
		for(int i = 0; i < variables.length; i++) {
			String varName = variables[i].name;
			for(int j = 0; j < i; j++) {
				if(variables[j].name.equals(varName)) {
					errors.add("Two variables have the same name: " + varName +
							variables[j].getErr() + variables[i].getErr());
				}
			}
		}
	}

	private static void validateFunctionSet(FunctionSection[] functions, VariableDeclaration[] scopeVariables, ErrorWrapper errors) {
		for(int i = 0; i < functions.length; i++) {
			FunctionSection func = functions[i];
			// check variable name conflicts
			for(VariableDeclaration var : scopeVariables) {
				if(var.name.equals(func.name))
					errors.add("A function has the same name as a variable: " +
							var.name + func.getErr() + var.getErr());
			}
			
			// check function duplicates
			for(int j = 0; j < i; j++) {
				if(functions[j].name.equals(func.name)) {
					errors.add("Two functions have the same name: " + func.name+
							func.getErr() + functions[j].getErr());
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
	}

	private static void validateStructConstructors(StructSection structure, ErrorWrapper errors) {
		for(int i = 0; i < structure.constructors.length; i++) {
			StructConstructor c1 = structure.constructors[i];
			
			// check constructor argument duplicates and mismatch
			for(int j = 0; j < c1.arguments.length; j++) {
				FunctionArgument arg1 = c1.arguments[j];
				VariableDeclaration member = structure.getMember(arg1.name);
				if(member == null) {
					errors.add("Constructor argument " + arg1.name + " does not refer to a "
							+ "member of this structure:" + arg1.getErr());
				} else if(!member.getType().equals(arg1.type)) {
					errors.add("Constructor argument " + arg1.name + " does not match type "
							+ member.getType() + " of structure member:" + arg1.getErr() + member.getErr());
				}
				for(int k = 0; k < j; k++) {
					FunctionArgument arg2 = c1.arguments[k];
					if(arg1.name.equals(arg2.name))
						errors.add("Duplicate constructor argument name:" + arg1.getErr() + arg2.getErr());
				}
			}
			
			for(int j = 0; j < i; j++) {
				StructConstructor c2 = structure.constructors[i];
				if(FunctionArguments.matchNoConversions(c1.getArgumentTypes(), c2.getArgumentTypes()))
					errors.add("Duplicate constructor found:" + c1.getErr() + c2.getErr());
			}
		}
	}

	private static void validateStructNull(StructSection structure, ErrorWrapper errors) {
		for(int i = 0; i < structure.nullFields.length; i++) {
			ConstructorDefaultValue f1 = structure.nullFields[i];
			VariableDeclaration member = structure.getMember(f1.name);
			if(member == null)
				errors.add("Null field " + f1.name + " does not refer to a member of"
						+ " this structure:" + f1.getErr());
			
			for(int j = 0; j < i; j++) {
				ConstructorDefaultValue f2 = structure.nullFields[j];
				if(f1.name.equals(f2.name))
					errors.add("Duplicate null field found:" + f1.getErr() + f2.getErr());
			}
		}
	}

	private static void validateStructOperators(StructSection structure, ErrorWrapper errors) {
		for(int i = 0; i < structure.operators.length; i++) {
			OverloadedOperator o1 = structure.operators[i];
			OverloadedOperatorPrototype op1 = o1.prototype;
			// check overload duplicates
			for(int j = 0; j < i; j++) {
				OverloadedOperator o2 = structure.operators[j];
				OverloadedOperatorPrototype op2 = o2.prototype;
				if(op1.loType.equals(op2.loType) && op1.roType.equals(op2.roType))
					errors.add("Duplicate operator found:" + o1.getErr() + o2.getErr());
			}
			// check types
			if(!(op1.loType instanceof VarStructType && ((VarStructType) op1.loType)
					.structure.matchesPrototype(structure.getPrototype())) &&
				!(op1.roType instanceof VarStructType && ((VarStructType) op1.roType)
					.structure.matchesPrototype(structure.getPrototype()))) {
				errors.add("Operator overloads must take at least one argument of"
						+ " the struct type holding them:" + o1.getErr());
			}
		}
	}

	private void validateAccessibleStructures(Unit unit, ErrorWrapper errors) {
		validateTypeAccesses(unit, linker.declaredStructures, errors);
	}

	private void validateAccessibleBlueprints(Unit unit, ErrorWrapper errors) {
		validateTypeAccesses(unit, linker.declaredBlueprints, errors);
	}
	
	private <T extends TypeAccess> void validateTypeAccesses(Unit unit, Map<String, T[]> declaredAccesses, ErrorWrapper errors) {
		List<T> accessibleAccesses = new ArrayList<>();
		accessibleAccesses.addAll(Arrays.asList(declaredAccesses.get(unit.fullBase)));
		for(String importation : unit.importations)
			accessibleAccesses.addAll(Arrays.asList(declaredAccesses.get(importation)));
		
		for(int i = 1; i < accessibleAccesses.size(); i++) {
			T s1 = accessibleAccesses.get(i);
			for(int j = 0; j < i; j++) {
				T s2 = accessibleAccesses.get(j);
				if(s1.getSignature().name.equals(s2.getSignature().name))
					errors.add("Two accessible types have the same name: "
							+ s1.getSignature() + " and "
							+ s2.getSignature());
			}
		}
	}
	
	private void validateBlueprintOperators(Blueprint bp, ErrorWrapper errors) {
		for(int i = 0; i < bp.operators.length; i++) {
			BlueprintOperator o1 = bp.operators[i];
			OverloadedOperatorPrototype op1 = o1.prototype;
			// check overload duplicates
			for(int j = 0; j < i; j++) {
				BlueprintOperator o2 = bp.operators[j];
				OverloadedOperatorPrototype op2 = o2.prototype;
				if(op1.loType.equals(op2.loType) && op1.roType.equals(op2.roType))
					errors.add("Duplicate operator found:" + o1.getErr() + o2.getErr());
			}
			// check types
			if(op1.loType != VarSelfType.SELF && op1.roType != VarSelfType.SELF) {
				errors.add("Blueprint operators must take at least one 'Self' argument:" + o1.getErr());
			}
		}
	}

}
