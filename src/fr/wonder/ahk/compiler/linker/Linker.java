package fr.wonder.ahk.compiler.linker;

import java.util.HashSet;
import java.util.Set;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiler.Natives;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.ahk.handles.CompiledHandle;
import fr.wonder.ahk.handles.LinkedHandle;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;
import fr.wonder.commons.utils.ArrayOperator;

public class Linker {
	
	/**
	 * @throws WrappedException if any unit cannot be linked
	 */
	public static LinkedHandle link(CompiledHandle handle, ErrorWrapper errors) throws WrappedException {
		// search for all required native units
		Set<String> nativesRequirements = new HashSet<>();
		for(Unit u : handle.units) {
			for(String i : u.importations) {
				if(i.startsWith(Natives.ahkImportBase))
					nativesRequirements.add(i);
			}
		}
		errors.assertNoErrors();
		
		Unit[] linkedNatives = Natives.getUnits(nativesRequirements, errors).toArray(Unit[]::new);
		Unit[] linkedUnits = ArrayOperator.add(handle.units, linkedNatives);
		String[] linkedUnitBases = ArrayOperator.map(linkedUnits, String[]::new, u -> u.fullBase);
		
		// check unit duplicates
		for(int u = 0; u < handle.units.length; u++) {
			Unit unit = handle.units[u];
			for(int j = 0; j < u; j++) {
				if(handle.units[j].fullBase.equals(unit.fullBase))
					errors.add("Duplicate unit found with base " + unit.fullBase);
			}
		}
		errors.assertNoErrors();
		
		// check all project unit importations (not native units)
		for(int i = 0; i < handle.units.length; i++) {
			Unit unit = handle.units[i];
			for(int j = 0; j < unit.importations.length; j++) {
				String importation = unit.importations[j];
				// search in the project & native units
				if(!ArrayOperator.contains(linkedUnitBases, importation))
					errors.add("Missing importation in unit " + unit.fullBase + " for " + importation);
			}
		}
		errors.assertNoErrors();
		
		// prelink units (project & natives)
		for(Unit unit : linkedUnits)
			prelinkUnit(unit, errors);
		errors.assertNoErrors();
		
		UnitPrototype[] prototypes = ArrayOperator.map(linkedUnits, UnitPrototype[]::new, u -> u.prototype);
		
		LinkedHandle linkedHandle = new LinkedHandle(linkedUnits, linkedNatives);
		
		// actually link unit statements and expressions
		for(int u = 0; u < handle.units.length; u++) {
			ErrorWrapper subErrors = errors.subErrrors("Unable to link unit " + linkedUnits[u].fullBase);
			linkUnit(linkedUnits[u], prototypes, linkedHandle.typesTable, subErrors);
		}
		errors.assertNoErrors();
		
		return linkedHandle;
	}
	
	/** Computes functions and variables signatures, validates units and sets unit.prototype */
	public static void prelinkUnit(Unit unit, ErrorWrapper errors) throws WrappedException {
		// TODO when struct types are implemented…
		// make sure that the function argument types and return type are linked BEFORE computing its signature
		for(FunctionSection func : unit.functions) {
			func.setSignature(new Signature(
					unit.fullBase,
					func.name,
					func.name + "_" +
					func.getFunctionType().getSignature()));
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
						errors.add("Two arguments have the same name" + func.getErr());
				}
			}
		}
		
		Prototypes.buildPrototype(unit);
		errors.assertNoErrors();
	}
	
	/**
	 * Assumes that the unit has been prelinked.
	 */
	public static void linkUnit(Unit unit, UnitPrototype[] units, TypesTable typesTable, ErrorWrapper errors) {
		UnitScope unitScope = new UnitScope(unit.prototype, unit.prototype.filterImportedUnits(units));
		for(int i = 0; i < unit.variables.length; i++) {
			VariableDeclaration var = unit.variables[i];
			ExpressionLinker.linkExpressions(unit, unitScope, var.getExpressions(), typesTable, errors);
		}
		
		for(int i = 0; i < unit.functions.length; i++) {
			FunctionSection func = unit.functions[i];
			ErrorWrapper ferrors = errors.subErrrors("Errors in function " + func.getSignature().computedSignature);
			StatementLinker.linkStatements(
					typesTable,
					unit,
					unitScope.innerScope(),
					func,
					ferrors);
		}
	}
	
}
