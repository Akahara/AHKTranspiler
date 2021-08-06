package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.expressions.ArrayExp;
import fr.wonder.ahk.compiled.expressions.ConstructorExp;
import fr.wonder.ahk.compiled.expressions.DirectAccessExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.FunctionCallExp;
import fr.wonder.ahk.compiled.expressions.FunctionExp;
import fr.wonder.ahk.compiled.expressions.FunctionExpression;
import fr.wonder.ahk.compiled.expressions.IndexingExp;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.Prototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.types.ConversionTable;
import fr.wonder.ahk.compiler.types.FunctionArguments;
import fr.wonder.ahk.compiler.types.Operation;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.utils.ArrayOperator;

class ExpressionLinker {

	static void linkExpressions(Unit unit, Scope scope, ExpressionHolder expressionHolder,
			TypesTable typesTable, ErrorWrapper errors) {
		
		Expression[] expressions = expressionHolder.getExpressions();
		for(int i = 0; i < expressions.length; i++) {
			Expression exp = expressions[i];
			
			if(exp == null)
				continue;
			
			linkExpressions(unit, scope, exp, typesTable, errors);
			
			if(exp instanceof VarExp) {
				linkVariableExpression(unit, scope, (VarExp) exp, errors);
				
			} else if(exp instanceof ArrayExp) {
				linkArrayExpression((ArrayExp) exp, errors);
				
			} else if(exp instanceof IndexingExp) {
				linkIndexingExpression((IndexingExp) exp, errors);
				
			} else if(exp instanceof FunctionCallExp) {
				exp = linkFunctionExpression(unit, expressions, i, errors);
				
			} else if(exp instanceof OperationExp) {
				linkOperationExpression((OperationExp) exp, typesTable, errors);
			
			} else if(exp instanceof ConstructorExp) {
				linkConstructorExpression(unit, (ConstructorExp) exp, errors);
				
			} else if(exp instanceof DirectAccessExp) {
				linkDirectAccessExp(unit, (DirectAccessExp) exp, errors);
				
			}
			
			exp.computeValueType(typesTable, errors);
		}
	}

	private static void linkVariableExpression(Unit unit, Scope scope, VarExp exp, ErrorWrapper errors) {
		// search for the variable/function declaration
		VarAccess var = scope.getVariable(exp.variable);
		if(var == null) {
			errors.add("Usage of undeclared variable " + exp.variable + exp.getErr());
			var = Invalids.VARIABLE_PROTO;
		} else if(!var.getSignature().declaringUnit.equals(unit.fullBase) && var.getSignature().declaringUnit != VarAccess.INNER_UNIT) {
			Prototype<?> proto = (Prototype<?>) var;
			unit.prototype.externalAccesses.add(proto);
		}
		exp.declaration = var;
	}

	private static void linkArrayExpression(ArrayExp exp, ErrorWrapper errors) {
		if(exp.getLength() == 0) {
			exp.type = VarArrayType.EMPTY_ARRAY;
		} else {
			Expression[] values = exp.getValues();
			VarType componentsType = values[0].getType();
			for(int j = 1; j < values.length; j++) {
				componentsType = ConversionTable.getCommonParent(componentsType, values[j].getType());
				if(componentsType == null)
					break;
			}
			if(componentsType == null) {
				errors.add("No shared type in array declaration" + exp.getErr());
				componentsType = Invalids.TYPE;
			}
			exp.type = new VarArrayType(componentsType);
		}
	}

	private static void linkIndexingExpression(IndexingExp exp, ErrorWrapper errors) {
		// validate types, there is no linking to do
		VarType arrayType = exp.getArray().getType();
		Expression[] indices = exp.getIndices();
		for(int j = 0; j < indices.length; j++) {
			if(arrayType instanceof VarArrayType) {
				arrayType = ((VarArrayType) arrayType).componentType;
			} else {
				errors.add("Type " + arrayType + " cannot be indexed" + exp.getErr());
				break;
			}
		}
		for(Expression index : indices) {
			if(index.getType() != VarType.INT)
				errors.add("An index must have type int" + index.getErr());
		}
	}

	private static void linkConstructorExpression(Unit unit, ConstructorExp exp, ErrorWrapper errors) {
		StructPrototype structure = exp.getType().structure;
		VarType[] args = ArrayOperator.map(exp.getExpressions(), VarType[]::new, Expression::getType);
		ConstructorPrototype[] accessibleConstructors = structure.constructors;
		if(!unit.fullBase.equals(structure.signature.declaringUnit))
			accessibleConstructors = ArrayOperator.filter(accessibleConstructors, c -> c.modifiers.visibility == DeclarationVisibility.GLOBAL);
		ConstructorPrototype matchingConstructor = FunctionArguments.searchMatchingConstructor(accessibleConstructors, args, exp, errors);
		exp.constructor = matchingConstructor == null ? Invalids.CONSTRUCTOR_PROTOTYPE : matchingConstructor;
	}

	private static void linkOperationExpression(OperationExp exp, TypesTable typesTable, ErrorWrapper errors) {
		Operation op = typesTable.getOperation(exp);
		// TODO when operator overloading is implemented...
		// if the operation refers to an overloaded operator add it to the external accesses of the unit prototype
		if(op == null) {
			errors.add("Unimplemented operation! " + exp.operationString() + exp.getErr());
			op = Invalids.OPERATION;
		}
		// #setOperation replaces the operation expression operands by cast expressions if needed
		exp.setOperation(op);
	}

	private static void linkDirectAccessExp(Unit unit, DirectAccessExp exp, ErrorWrapper errors) {
		VarType instanceType = exp.getStruct().getType();
		if(!(instanceType instanceof VarStructType)) {
			errors.add("Cannot access a member of an instance of type " + instanceType + exp.getErr());
			exp.member = Invalids.VARIABLE_PROTO;
			return;
		}
		StructPrototype prototype = ((VarStructType) instanceType).structure;
		VariablePrototype member = prototype.getMember(exp.memberName);
		if(member == null) {
			errors.add("Type " + prototype.getName() + " does not have a member named " + exp.memberName + exp.getErr());
			exp.member = Invalids.VARIABLE_PROTO;
			return;
		}
		// check if member is accessible
		if(!prototype.signature.declaringUnit.equals(unit.fullBase) &&
				member.modifiers.visibility == DeclarationVisibility.LOCAL) {
			errors.add("Member " + exp.memberName + " of structure " + prototype.getName()
					+ " is not accessible:" + exp.getErr());
			exp.member = Invalids.VARIABLE_PROTO;
			return;
		}
		// check if member can be accessed (may not be because of non-imported struct type)
		VarType memberType = member.type;
		if(memberType instanceof VarStructType && 
				!unit.prototype.isAccessibleStruct(((VarStructType) memberType).structure)) {
			errors.add("Member " + exp.memberName + " of structure " + prototype.getName()
					+ " has type " + memberType + " which cannot be accessed" + exp.getErr());
			exp.member = Invalids.VARIABLE_PROTO;
			return;
		}
		exp.member = member;
	}

	private static Expression linkFunctionExpression(Unit unit,
			Expression[] expressions, int expressionIndex, ErrorWrapper errors) {
		
		FunctionCallExp fexp = (FunctionCallExp) expressions[expressionIndex];
		VarFunctionType functionType;
		// if possible, replace the FunctionCallExp by a FunctionExp
		if(fexp.getFunction() instanceof VarExp && ((VarExp) fexp.getFunction()).declaration instanceof FunctionPrototype) {
			FunctionPrototype function = (FunctionPrototype) ((VarExp) fexp.getFunction()).declaration;
			FunctionExp functionExpression = new FunctionExp(unit.source, fexp, function);
			expressions[expressionIndex] = functionExpression;
			functionType = function.functionType;
		} else {
			if(!(fexp.getFunction().getType() instanceof VarFunctionType)) {
				// if invalid, an error was already reported, do not add another
				if(fexp.getFunction().getType() != Invalids.TYPE)
					errors.add("Type " + fexp.getFunction().getType() + " is not callable:" + fexp.getErr());
				fexp.functionType = Invalids.FUNCTION_TYPE;
				return fexp;
			} else {
				fexp.functionType = functionType = (VarFunctionType) fexp.getFunction().getType();
			}
		}
		FunctionExpression functionExpression = (FunctionExpression) expressions[expressionIndex];
		if(functionExpression.argumentCount() != functionType.arguments.length) {
			errors.add("Invalid number of arguments: function takes " 
					+ functionType.arguments.length + " but " 
					+ functionExpression.argumentCount() 
					+ " are given:" + functionExpression.getErr());
		} else {
			for(int j = 0; j < functionType.arguments.length; j++) {
				Linker.checkAffectationType(functionExpression,
						j, functionType.arguments[j], errors);
			}
		}
		
		return functionExpression;
	}

}
