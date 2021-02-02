package fr.wonder.ahk.compiler.linker;

import java.util.List;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiler.Natives;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.ahk.handles.AHKCompiledHandle;
import fr.wonder.ahk.handles.AHKLinkedHandle;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;
import fr.wonder.commons.utils.ArrayOperator;

public class Linker {
	
	public static AHKLinkedHandle link(AHKCompiledHandle handle, ErrorWrapper errors) throws WrappedException {
		// search for all required native units
		ArrayOperator<Unit> nativesRequirements = new ArrayOperator<>();
		for(Unit u : handle.units) {
			ErrorWrapper nativeImportErrors = errors.subErrrors("Missive native import in unit " + u.fullBase);
			nativesRequirements.addIfAbsent(getNativeRequirements(u.importations, nativeImportErrors));
		}
		errors.assertNoErrors();
		
		Unit[] linkedNatives = nativesRequirements.finish(Unit[]::new);
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
		prelinkUnits(linkedUnits, errors);
		errors.assertNoErrors();
		
		UnitPrototype[] prototypes = buildPrototypes(linkedUnits);
		
		AHKLinkedHandle linkedHandle = new AHKLinkedHandle(linkedUnits, linkedNatives);
		
		// actually link unit statements and expressions
		for(int u = 0; u < linkedUnits.length; u++) {
			ErrorWrapper subErrors = errors.subErrrors("Unable to link unit " + linkedUnits[u].fullBase);
			linkUnit(linkedHandle.typesTable, linkedUnits[u], prototypes, subErrors);
		}
		errors.assertNoErrors();
		
		return linkedHandle;
	}

	/** Computes functions and variables signatures, validates units */
	public static void prelinkUnits(Unit[] units, ErrorWrapper errors) {
		for(Unit unit : units) {
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
		}
	}
	
	/**
	 * @param importations array of unit full bases, non-native unit importations are ignored
	 */
	public static Unit[] getNativeRequirements(String[] importations, ErrorWrapper errors) throws WrappedException {
		ArrayOperator<Unit> nativeUnits = new ArrayOperator<>();
		for(String importation : importations) {
			if(importation.startsWith(Natives.ahkImportBase)) {
				List<Unit> importedUnits = Natives.getUnits(importation, errors);
				if(importedUnits != null)
					nativeUnits.addIfAbsent(importedUnits);
			}
		}
		errors.assertNoErrors();
		
		return nativeUnits.finish(Unit[]::new);
	}
	
	private static UnitPrototype[] buildPrototypes(Unit[] units) {
		UnitPrototype[] prototypes = new UnitPrototype[units.length];
		for(int j = 0; j < units.length; j++) {
			Unit unit = units[j];
			FunctionPrototype[] functions = new FunctionPrototype[unit.functions.length];
			for(int i = 0; i < functions.length; i++) {
				FunctionSection f = unit.functions[i];
				functions[i] = new FunctionPrototype(
						unit.fullBase,
						f.name,
						f.getSignature().computedSignature,
						f.getFunctionType());
			}
			VariablePrototype[] variables = new VariablePrototype[unit.variables.length];
			for(int i = 0; i < variables.length; i++) {
				VariableDeclaration v = unit.variables[i];
				variables[i] = new VariablePrototype(
						unit.fullBase,
						v.name,
						v.getSignature().computedSignature,
						v.getType());
			}
			unit.prototype = prototypes[j] = new UnitPrototype(
					unit.fullBase,
					unit.importations,
					functions,
					variables);
		}
		return prototypes;
	}
	
	/**
	 * Assumes that the unit has been prelinked.
	 */
	private static void linkUnit(TypesTable typesTable, Unit unit, UnitPrototype[] units, ErrorWrapper errors) {
		UnitScope unitScope = new UnitScope(unit.prototype, unit.prototype.filterImportedUnits(units));
		for(int i = 0; i < unit.variables.length; i++) {
			VariableDeclaration var = unit.variables[i];
			ExpressionLinker.linkExpressions(unit, unitScope, var.getExpressions(), typesTable, errors);
		}
		
		for(int i = 0; i < unit.functions.length; i++) {
			FunctionSection func = unit.functions[i];
			
			// link variables
			StatementLinker.linkStatements(typesTable, unit, unitScope.innerScope(),
					func, errors.subErrrors("Errors in function " + func.getSignature().computedSignature));
		}
	}
	
}
