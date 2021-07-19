package fr.wonder.ahk.compiler.linker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.Natives;
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
		
		Map<String, StructSection[]> declaredStructures = new HashMap<>();
		for(Unit u : linkedUnits)
			declaredStructures.put(u.fullBase, u.structures);
		
		// prelink units (project & natives)
		for(Unit unit : linkedUnits)
			Prelinker.prelinkUnit(unit, declaredStructures, errors.subErrrors("Unable to prelink unit " + unit.fullBase));
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
