package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.expressions.ArrayExp;
import fr.wonder.ahk.compiled.expressions.ConstructorExp;
import fr.wonder.ahk.compiled.expressions.ConversionExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.FunctionCallExp;
import fr.wonder.ahk.compiled.expressions.FunctionExp;
import fr.wonder.ahk.compiled.expressions.IndexingExp;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.types.ConversionTable;
import fr.wonder.ahk.compiler.types.FunctionArguments;
import fr.wonder.ahk.compiler.types.Operation;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnimplementedException;

class ExpressionLinker {

	// TODO use the ExpressionHolder interface
	static void linkExpressions(Unit unit, Scope scope, Expression[] expressions,
			TypesTable typesTable, ErrorWrapper errors) {
		
		for(int i = 0; i < expressions.length; i++) {
			Expression exp = expressions[i];
			
			linkExpressions(unit, scope, exp.expressions, typesTable, errors);
			
			if(exp instanceof VarExp) {
				// search for the variable/function declaration
				VarExp vexp = (VarExp) exp;
				VarAccess var = scope.getVariable(vexp.variable);
				String varUnit = var == null ? null : var.getSignature().declaringUnit;
				if(var == null) {
					errors.add("Usage of undeclared variable " + vexp.variable + vexp.getErr());
					var = Invalids.ACCESS;
				} else if(!varUnit.equals(unit.fullBase) && varUnit != VarAccess.INNER_UNIT) {
					unit.prototype.externalAccesses.add(var);
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
				FunctionCallExp fexp = (FunctionCallExp) exp;
				// TODO implement functions as variables
				if(fexp.getFunction() instanceof VarExp) {
					// replace the FunctionCallExp by a FunctionExp
					FunctionPrototype function = searchMatchingFunction(scope.getUnitScope(), fexp, typesTable, errors);
					if(function != Invalids.FUNCTION_PROTO) {
						if(!function.getSignature().declaringUnit.equals(unit.fullBase))
							unit.prototype.externalAccesses.add(function);
						FunctionExp functionExpression = new FunctionExp(unit.source, fexp, function);
						exp = expressions[i] = functionExpression;
						for(int j = 0; j < fexp.getArguments().length; j++) {
							Expression argument = functionExpression.getArguments()[j];
							if(function.functionType.arguments[j] != argument.getType()) {
								VarType converted = function.functionType.arguments[j];
								ConversionExp conv = new ConversionExp(argument, converted);
								functionExpression.expressions[j] = conv;
							}
						}
					}
				} else {
					// TODO implement functions as expressions
					throw new UnimplementedException("Unimplemented function call " + fexp.getErr());
				}
				
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
				VarStructType type = cexp.getType();
				// FIX search for the valid constructor
				cexp.constructor = type.structure.constructors[0];
			}
			
			exp.computeValueType(typesTable, errors);
		}
	}

	private static FunctionPrototype searchMatchingFunction(UnitScope unitScope, FunctionCallExp fexp,
			TypesTable typesTable, ErrorWrapper errors) {
		String funcName = ((VarExp) fexp.getFunction()).variable;
		VarType[] args = fexp.getArgumentsTypes();
		FunctionPrototype[] functions = unitScope.getFunctions(funcName);
		
		int validFuncConversionCount = Integer.MAX_VALUE;
		FunctionPrototype validFunc = Invalids.FUNCTION_PROTO;
		boolean multipleMatches = false;
		for(FunctionPrototype func : functions) {
			if(func.functionType.arguments.length != args.length)
				continue;
			int convertionCount = FunctionArguments.getMinimumConvertionCount(func.functionType.arguments, args);
			if(convertionCount == -1)
				continue;
			if(convertionCount == validFuncConversionCount) {
				validFunc = Invalids.FUNCTION_PROTO;
				multipleMatches = true;
			} else if(convertionCount < validFuncConversionCount) {
				validFunc = func;
				validFuncConversionCount = convertionCount;
				multipleMatches = false;
			}
		}
		if(multipleMatches) {
			errors.add("Multiple functions match given parameters:" + fexp.getErr());
		} else if(validFunc == Invalids.FUNCTION_PROTO) {
			errors.add("No matching function match given parameters:" + fexp.getErr());
		}
		return validFunc;
	}

}
