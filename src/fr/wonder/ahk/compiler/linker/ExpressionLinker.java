package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.expressions.ArrayExp;
import fr.wonder.ahk.compiled.expressions.ConstructorExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.FunctionCallExp;
import fr.wonder.ahk.compiled.expressions.FunctionExp;
import fr.wonder.ahk.compiled.expressions.FunctionExpression;
import fr.wonder.ahk.compiled.expressions.IndexingExp;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.Prototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
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
				// search for the variable/function declaration
				VarExp vexp = (VarExp) exp;
				VarAccess var = scope.getVariable(vexp.variable);
				if(var == null) {
					errors.add("Usage of undeclared variable " + vexp.variable + vexp.getErr());
					var = Invalids.VARIABLE_PROTO;
				} else if(!var.getSignature().declaringUnit.equals(unit.fullBase) && var.getSignature().declaringUnit != VarAccess.INNER_UNIT) {
					Prototype<?> proto = (Prototype<?>) var;
					unit.prototype.externalAccesses.add(proto);
				}
				vexp.declaration = var;
				
			} else if(exp instanceof ArrayExp) {
				ArrayExp array = (ArrayExp) exp;
				if(array.getLength() == 0) {
					array.type = VarArrayType.EMPTY_ARRAY;
				} else {
					Expression[] values = array.getValues();
					VarType componentsType = values[0].getType();
					for(int j = 1; j < values.length; j++) {
						componentsType = ConversionTable.getCommonParent(componentsType, values[j].getType());
						if(componentsType == null)
							break;
					}
					if(componentsType == null) {
						errors.add("No shared type in array declaration" + array.getErr());
						componentsType = Invalids.TYPE;
					}
					array.type = new VarArrayType(componentsType);
				}
				
			} else if(exp instanceof IndexingExp) {
				// validate types, there is no linking to do
				IndexingExp iexp = (IndexingExp) exp;
				VarType arrayType = iexp.getArray().getType();
				Expression[] indices = iexp.getIndices();
				for(int j = 0; j < indices.length; j++) {
					if(arrayType instanceof VarArrayType) {
						arrayType = ((VarArrayType) arrayType).componentType;
					} else {
						errors.add("Type " + arrayType + " cannot be indexed" + iexp.getErr());
						break;
					}
				}
				for(Expression index : indices) {
					if(index.getType() != VarType.INT)
						errors.add("An index must have type int" + index.getErr());
				}
				
			} else if(exp instanceof FunctionCallExp) {
				exp = linkFunctionExpression(unit, expressions, i, errors);
				
			} else if(exp instanceof OperationExp) {
				OperationExp oexp = (OperationExp) exp;
				Operation op = typesTable.getOperation(oexp);
				// TODO when operator overloading is implemented...
				// if the operation refers to an overloaded operator add it to the external accesses of the unit prototype
				if(op == null) {
					errors.add("Unimplemented operation! " + oexp.operationString() + oexp.getErr());
					op = Invalids.OPERATION;
				}
				// #setOperation replaces the operation expression operands by cast expressions if needed
				oexp.setOperation(op);
			
			} else if(exp instanceof ConstructorExp) {
				ConstructorExp cexp = (ConstructorExp) exp;
				StructPrototype structure = cexp.getType().structure;
				VarType[] args = ArrayOperator.map(cexp.getExpressions(), VarType[]::new, Expression::getType);
				ConstructorPrototype matchingConstructor = FunctionArguments.searchMatchingCallable(structure.constructors, args, cexp, errors);
				cexp.constructor = matchingConstructor == null ? Invalids.CONSTRUCTOR_PROTOTYPE : matchingConstructor;
				
			}
			
			exp.computeValueType(typesTable, errors);
		}
	}

	private static Expression linkFunctionExpression(Unit unit, Expression[] expressions, int expressionIndex, ErrorWrapper errors) {
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
