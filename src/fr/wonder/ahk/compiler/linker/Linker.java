package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.expressions.ConversionExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.NullExp;
import fr.wonder.ahk.compiled.expressions.types.VarNullType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.UnitCompilationState;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.types.ConversionTable;
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
		
		// actually link unit statements and expressions
		// only link non-native units, natives are already linked
		for(Unit unit : handle.units) {
			ErrorWrapper subErrors = errors.subErrrors("Unable to link unit " + unit.fullBase);
			linkUnit(unit, prototypes, subErrors);
		}
		errors.assertNoErrors();
		
		return new LinkedHandle(handle.units);
	}

	public static void assertNoDuplicates(Unit[] units, ErrorWrapper errors) throws WrappedException {
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
	
	public static void assertNoMissingImportation(Unit[] units, ErrorWrapper errors) throws WrappedException {
		Object[] bases = ArrayOperator.map(units, u->u.fullBase);
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
		// compute signatures before listing prototypes
		// at this point their still might be name collisions, that will cause
		// signature collisions but they will be detected by Prelinker#prelinkUnit
		for(Unit unit : units) {
			
			if(unit.compilationState != UnitCompilationState.PARSED)
				throw new IllegalStateException("Cannot prelink an unit with state " + unit.compilationState);
			unit.compilationState = UnitCompilationState.PRELINKED_WITH_ERRORS;
			
			Prelinker.computeSignaturesAndPrototypes(unit);
		}
		
		UnitPrototype[] prototypes = ArrayOperator.map(units, UnitPrototype[]::new, u->u.prototype);
		
		for(Unit unit : units) {
			ErrorWrapper subErrors = errors.subErrrors("Unable to prelink unit " + unit.fullBase);
			Prelinker.prelinkUnit(unit, prototypes, subErrors);
			if(subErrors.noErrors())
				unit.compilationState = UnitCompilationState.PRELINKED;
		}
		
		// collect overloaded operators and check for duplicates
		for(UnitPrototype u : prototypes) {
			collectOverloadedOperators(u, typesTable, errors);
		}
		
		errors.assertNoErrors();
	}
	
	private static void collectOverloadedOperators(UnitPrototype unit,
			TypesTable typesTable, ErrorWrapper errors) {
		
		for(StructPrototype struct : unit.structures) {
			for(OverloadedOperatorPrototype oop : struct.overloadedOperators) {
				OverloadedOperatorPrototype overridden = typesTable.operations.registerOperation(oop);
				
				if(overridden != null) {
					errors.add("Two operators overloads conflict: in " +
							oop.signature.declaringUnit + " and " +
							overridden.signature.declaringUnit + ": " + oop);
				}
			}
		}
	}
	
	static 		TypesTable typesTable = new TypesTable(); // FIX
	
	/**
	 * Assumes that the unit has been prelinked.
	 */
	public static void linkUnit(Unit unit, UnitPrototype[] prototypes, ErrorWrapper errors) {
		if(unit.compilationState != UnitCompilationState.PRELINKED)
			throw new IllegalStateException("Cannot link an unit with state " + unit.compilationState);
		
		unit.compilationState = UnitCompilationState.LINKED_WITH_ERRORS;
		
		UnitScope unitScope = new UnitScope(unit.prototype, unit.prototype.filterImportedUnits(prototypes));
		
		for(VariableDeclaration var : unit.variables) {
			ExpressionLinker.linkExpressions(unit, unitScope, var, typesTable, errors);
			checkAffectationType(var, 0, var.getType(), errors);
		}
		
		for(FunctionSection func : unit.functions) {
			ErrorWrapper ferrors = errors.subErrrors("Errors in function " + func.getSignature().computedSignature);
			StatementLinker.linkStatements(
					unit,
					unitScope.innerScope(),
					func,
					typesTable,
					ferrors);
		}
		
		for(StructSection struct : unit.structures) {
			StructureLinker.linkStructure(unit, unitScope, typesTable, struct, errors);
		}
		
		if(errors.noErrors())
			unit.compilationState = UnitCompilationState.LINKED;
	}

	/**
	 * <p>
	 * Checks if the expression at index {@code valueIndex} of
	 * {@code valueHolder.getExpressions} can be affected to type {@code validType},
	 * if an implicit the expression is replaced by a {@link ConversionExp}. If the
	 * expression cannot be casted an error is reported.
	 * 
	 * <p>
	 * If the expression is a {@link NullExp} its "actual type" is set to
	 * {@code validType} if possible, otherwise an error is reported (see
	 * {@link VarNullType#isAcceptableNullType(VarType)}).
	 * 
	 * <p>
	 * The value of {@code valueIndex} depends on the {@link ExpressionHolder}
	 * implementation, for instance when checking a {@link VariableDeclaration} the
	 * expression to check is the only one of the statement hence {@code valueIndex}
	 * must be 0. For function calls this method will be used once for each argument
	 * with {@code valueIndex} ranging from 0 to the number of arguments -1.
	 */
	static void checkAffectationType(ExpressionHolder valueHolder, int valueIndex, VarType validType, ErrorWrapper errors) {
		Expression value = valueHolder.getExpressions()[valueIndex];
		if(value instanceof NullExp) {
			if(!VarNullType.isAcceptableNullType(validType)) {
				errors.add("Type mismatch, cannot use null with type " + validType + valueHolder.getErr());
			} else {
				((NullExp) value).setNullType(validType);
			}
			return;
		}
		if(value.getType().equals(validType))
			return;
		if(ConversionTable.canConvertImplicitely(value.getType(), validType)) {
			value = new ConversionExp(value, validType);
			valueHolder.getExpressions()[valueIndex] = value;
		} else {
			errors.add("Type mismatch, cannot convert " + value.getType() + " to " + validType + value.getErr());
		}
	}
	
}
