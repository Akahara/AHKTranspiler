package fr.wonder.ahk.compiler.linker;

import static fr.wonder.commons.utils.ArrayOperator.map;

import java.util.HashMap;
import java.util.Map;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.UnitCompilationState;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.StructSection;
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
		assertNoDuplicates(handle.units, errors);
		assertNoMissingImportation(handle.units, errors);
		
		// prelink units (project & natives)
		prelinkUnits(handle.units, errors);
		
		UnitPrototype[] prototypes = ArrayOperator.map(handle.units, UnitPrototype[]::new, u -> u.prototype);
		TypesTable typesTable = new TypesTable();
		
		// actually link unit statements and expressions
		// only link non-native units, natives are already linked
		for(Unit unit : handle.units) {
			ErrorWrapper subErrors = errors.subErrrors("Unable to link unit " + unit.fullBase);
			linkUnit(unit, prototypes, typesTable, subErrors);
		}
		errors.assertNoErrors();
		
		return new LinkedHandle(handle.units, typesTable);
	}
	
	private static void assertNoDuplicates(Unit[] units, ErrorWrapper errors) throws WrappedException {
		// check unit duplicates
		for(int u = 0; u < units.length; u++) {
			Unit unit = units[u];
			for(int j = 0; j < u; j++) {
				if(units[j].fullBase.equals(unit.fullBase))
					errors.add("Duplicate unit found with base " + unit.fullBase);
			}
		}
		errors.assertNoErrors();
	}
	
	private static void assertNoMissingImportation(Unit[] units, ErrorWrapper errors) throws WrappedException {
		Object[] bases = map(units, u->u.fullBase);
		for(int i = 0; i < units.length; i++) {
			Unit unit = units[i];
			for(int j = 0; j < unit.importations.length; j++) {
				String importation = unit.importations[j];
				// search in the project & native units
				if(!ArrayOperator.contains(bases, importation))
					errors.add("Missing importation in unit " + unit.fullBase + " for " + importation);
			}
		}
		errors.assertNoErrors();
	}
	
	private static void prelinkUnits(Unit[] units, ErrorWrapper errors) throws WrappedException {
		Map<String, StructSection[]> declaredStructures = new HashMap<>();
		for(Unit u : units)
			declaredStructures.put(u.fullBase, u.structures);
		
		for(Unit unit : units) {
			ErrorWrapper subErrors = errors.subErrrors("Unable to prelink unit " + unit.fullBase);
			Prelinker.prelinkUnit(unit, declaredStructures, subErrors);
		}
		errors.assertNoErrors();
	}
	
	/**
	 * Assumes that the unit has been prelinked.
	 */
	public static void linkUnit(Unit unit, UnitPrototype[] units, TypesTable typesTable, ErrorWrapper errors) {
		if(unit.compilationState != UnitCompilationState.PRELINKED)
			throw new IllegalStateException("Cannot link an unit with state " + unit.compilationState);
		
		unit.compilationState = UnitCompilationState.LINKED_WITH_ERRORS;
		
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
		
		if(errors.noErrors())
			unit.compilationState = UnitCompilationState.LINKED;
	}
	
}
