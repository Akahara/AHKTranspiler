package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.expressions.types.VarSelfType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.prototypes.blueprints.BlueprintImplementation;
import fr.wonder.ahk.compiled.units.prototypes.blueprints.BlueprintPrototype;
import fr.wonder.ahk.compiled.units.sections.ConstructorDefaultValue;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiled.units.sections.OverloadedOperator;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnimplementedException;

class StructureLinker {

	private final Linker linker;
	
	StructureLinker(Linker linker) {
		this.linker = linker;
	}

	void prelinkStructure(Unit unit, StructSection struct, ErrorWrapper errors) {
		for(OverloadedOperator operator : struct.operators) {
			linkOperator(unit, struct, operator, errors);
		}
	}
	
	/**
	 * Operators are linked after types and prototypes were computed
	 * but before expressions, this way operator expressions can safely
	 * refer to operator's functions.
	 */
	private void linkOperator(Unit unit, StructSection struct, OverloadedOperator operator, ErrorWrapper errors) {
		operator.prototype.function = Invalids.FUNCTION_PROTO;
		FunctionPrototype func = unit.prototype.getFunction(operator.functionName);
		if(func == null) {
			errors.add("Undefined function for operator overload:" + operator.getErr());
			return;
		} else if(!DeclarationVisibility.isHigherOrder(func.modifiers.visibility, struct.modifiers.visibility)) {
			errors.add("Operator overload function has lower visibility than the operator itself:" + operator.getErr());
			return;
		}
		OverloadedOperatorPrototype op = operator.prototype;
		op.function = func;
		
		VarType[] args = func.functionType.arguments;
		if(args.length != op.argCount()) {
			errors.add("Invalid function for operator overload, operator takes " + op.argCount() +
					" but function " + func.getName() + " takes " + args.length + operator.getErr());
			return;
		}
		VarType funcLO = args.length > 1 ? args[0] : null;
		VarType funcRO = args.length > 1 ? args[1] : args[0];
		if(funcLO != null && !funcLO.equals(op.loType))
			errors.add("Invalid function for operator overload, left operand type mismatch " +
					op.loType + " against " + funcLO + operator.getErr());
		if(!funcRO.equals(op.roType))
			errors.add("Invalid function for operator overload, left operand type mismatch " +
					op.roType + " against " + funcRO + operator.getErr());
	}
	
	void linkStructure(Unit unit, UnitScope unitScope, StructSection struct, ErrorWrapper errors) {
		for(ConstructorDefaultValue nullField : struct.nullFields) {
			linker.expressions.linkExpressions(unit, unitScope, nullField, struct.genericContext, errors);
			VariableDeclaration member = struct.getMember(nullField.name);
			linker.checkAffectationType(nullField, 0, member.getType(), errors);
		}
		for(VariableDeclaration member : struct.members) {
			linker.expressions.linkExpressions(unit, unitScope, member, struct.genericContext, errors);
			linker.checkAffectationType(member, 0, member.getType(), errors);
		}
		for(BlueprintImplementation bpImpl : struct.implementedBlueprints) {
			linkBlueprintImplementation(struct, bpImpl, errors.subErrors("Invalid implementation of blueprint " + bpImpl.bpRef.name));
		}
	}

	public void collectOverloadedOperators(ErrorWrapper errors) {
		for(UnitPrototype u : linker.prototypes) {
			for(StructPrototype struct : u.structures) {
				for(OverloadedOperatorPrototype oop : struct.overloadedOperators) {
					OverloadedOperatorPrototype overridden = linker.typesTable.registerOverloadedOperator(oop);
					
					if(overridden != null) {
						errors.add("Two operators overloads conflict: in " +
								oop.signature.declaringUnit + " and " +
								overridden.signature.declaringUnit + ": " + oop);
					}
				}
			}
		}
	}
	
	private void linkBlueprintImplementation(StructSection struct,
			BlueprintImplementation bpImpl, ErrorWrapper errors) {
		
		BlueprintPrototype bp = bpImpl.bpRef.blueprint;
		
		bpImpl.variables = new VariablePrototype[bp.variables.length];
		for(int i = 0; i < bp.variables.length; i++)
			bpImpl.variables[i] = searchBPIVariable(struct, bp.variables[i], errors);

		bpImpl.functions = new FunctionPrototype[bp.functions.length];
		for(int i = 0; i < bp.functions.length; i++)
			bpImpl.functions[i] = searchBPIFunction(struct, bp.functions[i], errors);
		
		bpImpl.operators = new OverloadedOperatorPrototype[bp.operators.length];
		for(int i = 0; i < bp.operators.length; i++)
			bpImpl.operators[i] = searchBPIOperator(struct, bp.operators[i], errors);
	}
	
	private VariablePrototype searchBPIVariable(StructSection struct, VariablePrototype var, ErrorWrapper errors) {
		VariableDeclaration svar = struct.getMember(var.getName());
		if(svar == null) {
			errors.add("Missing variable " + var.getName() + " of type " + var.getType());
			return Invalids.VARIABLE_PROTO;
		}
		if(!bpiTypesMatch(struct, svar.getType(), var.getType())) {
			errors.add("Blueprint requires variable " + var.getName() + " to be of type " + var.getType() + svar.getErr());
			return Invalids.VARIABLE_PROTO;
		}
		return svar.getPrototype();
	}
	
	private FunctionPrototype searchBPIFunction(StructSection struct, FunctionPrototype func, ErrorWrapper errors) {
		throw new UnimplementedException("Unimplemented struct functions"); // TODO implement functions in structures
	}
	
	private OverloadedOperatorPrototype searchBPIOperator(StructSection struct, OverloadedOperatorPrototype op, ErrorWrapper errors) {
		for(OverloadedOperator sop : struct.operators) {
			OverloadedOperatorPrototype sopp = sop.prototype;
			if(sopp.operator == op.operator && sopp.hasLeftOperand() == op.hasLeftOperand() &&
					(!sopp.hasLeftOperand() || bpiTypesMatch(struct, sopp.loType, op.roType)) &&
					bpiTypesMatch(struct, sopp.roType, op.roType) &&
					bpiTypesMatch(struct, sopp.resultType, op.resultType))
				return sop.prototype;
		}
		errors.add("Blueprint requires an operator (" + op.toString() + ")");
		return Invalids.OVERLOADED_OPERATOR.prototype;
	}
	
	private static boolean bpiTypesMatch(StructSection struct, VarType implType, VarType bpType) {
		if(bpType == VarSelfType.SELF)
			return implType instanceof VarStructType && ((VarStructType) implType).structure.matchesPrototype(struct.getPrototype());
		return implType.equals(bpType);
	}

}