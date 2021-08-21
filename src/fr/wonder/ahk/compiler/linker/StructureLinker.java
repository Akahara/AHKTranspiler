package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.sections.ConstructorDefaultValue;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiled.units.sections.OverloadedOperator;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

class StructureLinker {

	private final Linker linker;
	
	StructureLinker(Linker linker) {
		this.linker = linker;
	}
	
	void linkStructure(Unit unit, UnitScope unitScope, TypesTable typesTable, StructSection struct, ErrorWrapper errors) {
		for(ConstructorDefaultValue nullField : struct.nullFields) {
			linker.expressions.linkExpressions(unit, unitScope, nullField, typesTable, errors);
			VariableDeclaration member = struct.getMember(nullField.name);
			linker.checkAffectationType(nullField, 0, member.getType(), errors);
		}
		for(VariableDeclaration member : struct.members) {
			linker.expressions.linkExpressions(unit, unitScope, member, typesTable, errors);
			linker.checkAffectationType(member, 0, member.getType(), errors);
		}
		for(OverloadedOperator operator : struct.operators) {
			linkOperator(unit, struct, operator, errors);
		}
	}
	
	private void linkOperator(Unit unit, StructSection struct, OverloadedOperator operator, ErrorWrapper errors) {
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

}