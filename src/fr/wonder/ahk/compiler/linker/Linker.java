package fr.wonder.ahk.compiler.linker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.wonder.ahk.compiled.expressions.ConversionExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.NullExp;
import fr.wonder.ahk.compiled.expressions.types.VarNullType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.Compiler;
import fr.wonder.ahk.compiler.types.ConversionTable;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.ahk.handles.CompiledHandle;
import fr.wonder.ahk.handles.LinkedHandle;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;
import fr.wonder.commons.utils.ArrayOperator;

public class Linker {
	
	final Unit[] units;
	final TypesTable typesTable = new TypesTable();
	
	final Prelinker prelinker;
	
	final ExpressionLinker expressions;
	final StatementLinker statements;
	final StructureLinker structures;
	
	// set by #prelinkUnits
	UnitPrototype[] prototypes;
	Map<String, List<StructPrototype>> declaredStructures;
	
	
	public Linker(CompiledHandle handle) {
		this.units = handle.units;
		this.prelinker = new Prelinker(this);
		this.expressions = new ExpressionLinker(this);
		this.statements = new StatementLinker(this);
		this.structures = new StructureLinker(this);
	}
	
	public LinkedHandle link(ErrorWrapper errors) throws WrappedException {
		Compiler.assertNoDuplicates(units, errors);
		Compiler.assertNoMissingImportation(units, errors);
		
		// prelink units (project & natives)
		prelinkUnits(errors);
		
		// actually link unit statements and expressions
		// only link non-native units, natives are already linked
		for(Unit unit : units) {
			ErrorWrapper subErrors = errors.subErrrors("Unable to link unit " + unit.fullBase);
			linkUnit(unit, subErrors);
		}
		errors.assertNoErrors();
		
		return new LinkedHandle(units);
	}

	private void prelinkUnits(ErrorWrapper errors) throws WrappedException {
		// compute signatures before listing prototypes
		// at this point their still might be name collisions, that will cause
		// signature collisions but they will be detected by Prelinker#prelinkUnit
		for(Unit unit : units)
			Prelinker.computeSignaturesAndPrototypes(unit);
		
		this.prototypes = ArrayOperator.map(units, UnitPrototype[]::new, u -> u.prototype);
		this.declaredStructures = new HashMap<>();
		
		// collect global structures
		for(UnitPrototype u : prototypes) {
			List<StructPrototype> globalStructures = new ArrayList<>();
			for(StructPrototype s : u.structures) {
				if(s.modifiers.visibility == DeclarationVisibility.GLOBAL)
					globalStructures.add(s);
			}
			declaredStructures.put(u.fullBase, globalStructures);
		}
		
		for(Unit unit : units) {
			prelinker.prelinkUnit(unit, errors.subErrrors("Unable to prelink unit " + unit.fullBase));
		}
		
		structures.collectOverloadedOperators(errors);
		
		errors.assertNoErrors();
	}
	
	/**
	 * Assumes that the unit has been prelinked.
	 */
	private void linkUnit(Unit unit, ErrorWrapper errors) {
		UnitScope unitScope = new UnitScope(unit.prototype, unit.prototype.filterImportedUnits(prototypes));
		
		for(VariableDeclaration var : unit.variables) {
			expressions.linkExpressions(unit, unitScope, var, typesTable, errors);
			checkAffectationType(var, 0, var.getType(), errors);
		}
		
		for(FunctionSection func : unit.functions) {
			ErrorWrapper ferrors = errors.subErrrors("Errors in function " + func.getSignature().computedSignature);
			statements.linkStatements(
					unit,
					unitScope.innerScope(),
					func,
					typesTable,
					ferrors);
		}
		
		for(StructSection struct : unit.structures) {
			structures.linkStructure(unit, unitScope, typesTable, struct, errors);
		}
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
	void checkAffectationType(ExpressionHolder valueHolder, int valueIndex,
			VarType validType, ErrorWrapper errors) {
		
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
	
	/**
	 * Checks if the given unit can use the given type.
	 * <p>
	 * When the unit somehow accesses a type it may not be usable because the unit
	 * is missing a struct type used by a function argument...
	 * <p>
	 * For example this won't work:
	 * 
	 * <blockquote><pre>
	 * unit A;
	 * 
	 * struct A {...}
	 * 
	 * -----------------
	 * unit Ap;
	 * 
	 * struct A {...} // note that the names are the same
	 * 
	 * -----------------
	 * unit B;
	 * import Ap;
	 * 
	 * func A f() {...} // 'A' results in Ap.A
	 * 
	 * -----------------
	 * unit C;
	 * import A;
	 * 
	 * func main() {
	 *    A a = ...;
	 *    f(a); // will fail because 'A' is A.A here and not Ap.A
	 * }
	 * </pre></blockquote>
	 * 
	 * @param unit the unit accessing {@code type}
	 * @param type the type to use
	 * @param elem the element accessing {@code type}, for logging purposes
	 * @param errors error wrapper
	 * 
	 * @return true iff {@code type} can safely be used
	 */
	boolean requireType(Unit unit, VarType type, SourceElement elem, ErrorWrapper errors) {
		Set<VarStructType> effectiveTypes = new HashSet<>();
		collectEffectiveTypes(type, effectiveTypes);
		
		boolean canBeUsed = true;
		
		for(VarStructType t : effectiveTypes) {
			StructPrototype sproto = t.structure;
			StructPrototype uproto = unit.prototype.getExternalStruct(t.name);
			
			if(uproto == null) {
				uproto = searchStructSection(unit, t.name);
				if(uproto != null && uproto.signature.declaringUnit.equals(unit.fullBase)) {
					unit.prototype.externalAccesses.add(uproto);
				}
			}
			
			if(uproto == null) {
				// the type is not imported by this unit
				errors.add("Cannot make use of type " + type + ", type " + t + " is not accessible:" + elem.getErr());
				canBeUsed = false;
				
			} else if(!sproto.matchesPrototype(uproto)) {
				// a type with the same name is accessible but is declared by another unit
				errors.add("Cannot make use of type " + type + ", type " + sproto + " type does not match "
						+ uproto + ":" + elem.getErr());
				canBeUsed = false;
			}
		}
		return canBeUsed;
	}
	
	/**
	 * collects the set of structure types used by {@code type} into
	 * {@code effectiveTypes}. If {@code type} is itself a struct type it is added
	 * to the list, otherwise its sub types are collected. For example a function of
	 * type {@code void:(Struct, StructB[])} has ({@code Struct, StructB}) as
	 * effective types.
	 * 
	 * @see VarType#getSubTypes()
	 */
	private static void collectEffectiveTypes(VarType type, Set<VarStructType> effectiveTypes) {
		if(type instanceof VarStructType) {
			effectiveTypes.add((VarStructType) type);
		} else {
			for(VarType st : type.getSubTypes()) {
				collectEffectiveTypes(st, effectiveTypes);
			}
		}
	}
	
	/**
	 * Returns an accessible structure with the given name, null if none match.<br>
	 * A structure is accessible if it is local to the given unit or global and
	 * imported.
	 */
	StructPrototype searchStructSection(Unit unit, String name) {
		for(StructSection structure : unit.structures) {
			if(structure.name.equals(name))
				return structure.getPrototype();
		}
		for(String imported : unit.importations) {
			List<StructPrototype> structures = declaredStructures.get(imported);
			for(StructPrototype structure : structures) {
				if(structure.getName().equals(name) && (
						structure.signature.declaringUnit.equals(unit.fullBase) ||
						structure.modifiers.visibility == DeclarationVisibility.GLOBAL))
					return structure;
			}
		}
		return null;
	}
	
}
