package fr.wonder.ahk.compiler.linker;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.wonder.ahk.compiled.expressions.ConversionExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.NullExp;
import fr.wonder.ahk.compiled.expressions.UninitializedArrayExp;
import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarNullType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.EnumPrototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.TypeAccess;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.LambdaClosureArgument;
import fr.wonder.ahk.compiled.units.sections.SimpleLambda;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.Compiler;
import fr.wonder.ahk.compiler.optimization.UnitOptimizer;
import fr.wonder.ahk.compiler.parser.StatementParser;
import fr.wonder.ahk.compiler.types.ConversionTable;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.ahk.handles.CompiledHandle;
import fr.wonder.ahk.handles.LinkedHandle;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;
import fr.wonder.commons.utils.ArrayOperator;

public class Linker {
	
	final CompiledHandle projectHandle;
	final Unit[] units;
	final TypesTable typesTable = new TypesTable();
	
	final Prelinker prelinker;
	
	final ExpressionLinker expressions;
	final StatementLinker statements;
	final StructureLinker structures;
	
	// set by #prelinkUnits
	UnitPrototype[] prototypes;
	Map<String, StructPrototype[]> declaredStructures;
	Map<String, EnumPrototype[]> declaredEnums;
	
	public Linker(CompiledHandle handle) {
		this.projectHandle = handle;
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
		for(Unit unit : units) {
			ErrorWrapper subErrors = errors.subErrors("Unable to link unit " + unit.fullBase);
			linkUnit(unit, subErrors);
		}
		
		errors.assertNoErrors();
		
		for(Unit unit : units) {
			ErrorWrapper subErrors = errors.subErrors("Unable to optimize unit " + unit.fullBase);
			UnitOptimizer.optimize(projectHandle, unit, subErrors);
		}
		
		errors.assertNoErrors();
		
		return new LinkedHandle(units, projectHandle.manifest);
	}

	private void prelinkUnits(ErrorWrapper errors) throws WrappedException {
		// compute signatures before listing prototypes
		// at this point their still might be name collisions, that will cause
		// signature collisions but they will be detected by Prelinker#prelinkUnit
		for(Unit unit : units)
			Prelinker.computeSignaturesAndPrototypes(unit);
		
		this.prototypes = ArrayOperator.map(units, UnitPrototype[]::new, u -> u.prototype);
		this.declaredStructures = new HashMap<>();
		this.declaredEnums = new HashMap<>();
		
		// collect global structures
		for(UnitPrototype u : prototypes) {
			declaredStructures.put(u.fullBase, u.structures);
			declaredEnums.put(u.fullBase, u.enums);
		}
		
		for(Unit unit : units) {
			prelinker.prelinkTypes(unit, errors.subErrors("Unable to prelink unit " + unit.fullBase + " types"));
		}
		
		for(Unit unit : units) {
			prelinker.prelinkUnit(unit, errors.subErrors("Unable to prelink unit " + unit.fullBase));
		}
		
		for(Unit unit : units) {
			for(StructSection struct : unit.structures) {
				structures.prelinkStructure(unit, struct, errors);
			}
		}
		
		structures.collectOverloadedOperators(errors);
		
		errors.assertNoErrors();
	}
	
	/**
	 * Assumes that the unit has been prelinked.
	 */
	private void linkUnit(Unit unit, ErrorWrapper errors) {
		UnitScope unitScope = new UnitScope(unit.prototype, unit.prototype.filterImportedUnits(prototypes));
		
		for(StructSection struct : unit.structures) {
			structures.linkStructure(unit, unitScope, struct, errors.subErrors("Errors in structure " + struct.name));
		}
		
		for(VariableDeclaration var : unit.variables) {
			expressions.linkExpressions(unit, unitScope, var, errors);
			checkAffectationType(var, 0, var.getType(), errors);
		}
		
		for(FunctionSection func : unit.functions) {
			ErrorWrapper ferrors = errors.subErrors("Errors in function " + func.getSignature().computedSignature);
			statements.linkStatements(unit, unitScope.innerScope(), func, ferrors);
		}
	}

	void linkLambda(Unit unit, Scope currentScope, SimpleLambda lambda, ErrorWrapper errors) {
		Scope lambdaScope = currentScope.getUnitScope().innerScope();
		for(FunctionArgument arg : lambda.args)
			lambdaScope.registerVariable(arg, arg, errors);
		for(LambdaClosureArgument arg : lambda.closureArguments) {
			arg.setOriginalVariable(currentScope.getVariable(arg.getVarName(), arg, errors));
			lambdaScope.registerVariable(arg, arg, errors);
		}
		expressions.linkExpressions(unit, lambdaScope, lambda, errors);
		checkAffectationType(lambda, 0, lambda.getReturnType(), errors);
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
		
		if(tryCheckSetableAffectationType(value.type, validType, value, errors))
			return;
		
//		if(validType instanceof VarStructType && ((VarStructType) validType).structure.hasGenericBindings()) {
//			errors.add("Type " + validType + " must be parameterized before affectation:" + valueHolder.getErr());
//			return;
//		}
		
		if(value.type == VarArrayType.EMPTY_ARRAY) {
			if(!(value instanceof UninitializedArrayExp)) {
				throw new IllegalStateException("Invalid expression holding empty array type " + value.getClass());
			} else if(validType instanceof VarArrayType) {
				value.type = validType; // update array type safely
				VarType componentType = ((VarArrayType) validType).componentType;
				Expression defaultComponentValue = StatementParser.getDefaultValue(
						componentType, value.sourceRef.source, value.sourceRef.start);
				if(!tryCheckSetableAffectationType(defaultComponentValue.type, componentType, value, errors))
					errors.add("Invalid component type for empty array: " + componentType + value.getErr());
				((UninitializedArrayExp) value).setDefaultComponentValue(defaultComponentValue);
			} else {
				errors.add("Type mismatch, cannot use an array type for " + validType + value.getErr());
			}
			return;
		}
		
		if(ConversionTable.canConvertImplicitly(value.getType(), validType)) {
			value = new ConversionExp(value, validType);
			valueHolder.getExpressions()[valueIndex] = value;
		} else {
			errors.add("Type mismatch, cannot convert " + value.getType() + " to " + validType + value.getErr());
		}
	}
	
	/**
	 * Has the same function as {@link #checkAffectationType(ExpressionHolder, int, VarType, ErrorWrapper)}
	 * but works with types without expressions. This is required to validate {@code null} uses
	 */
	private boolean tryCheckSetableAffectationType(VarType provided, VarType required, SourceElement queryElement, ErrorWrapper errors) {
		if(provided.equals(required))
			return true;
		
		if(provided instanceof VarNullType) {
			if(VarNullType.isAcceptableNullType(required)) {
				((VarNullType) provided).setActualType(required);
			} else {
				errors.add("Type mismatch, cannot use null with type " + required + queryElement.getErr());
			}
			return true;
		}
		
		return false;
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
			StructPrototype sproto = t.getBackingType();
			StructPrototype uproto = unit.prototype.getExternalStruct(t.getName());
			
			if(uproto == null) {
				uproto = searchStructSection(unit, t.getName());
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
		return searchTypeAccess(unit, declaredStructures, name);
	}

	EnumPrototype searchEnumSection(Unit unit, String name) {
		return searchTypeAccess(unit, declaredEnums, name);
	}

	private <T extends TypeAccess> T searchTypeAccess(Unit unit, Map<String, T[]> declaredAccesses, String name) {
		for(T t : declaredAccesses.get(unit.fullBase)) {
			if(t.getSignature().name.equals(name))
				return t;
		}
		for(String imported : unit.importations) {
			for(T t : declaredAccesses.get(imported)) {
				if(t.getSignature().name.equals(name) &&
					t.getModifiers().visibility == DeclarationVisibility.GLOBAL)
					return t;
			}
		}
		return null;
	}
	
}
