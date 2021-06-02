package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.expressions.ArrayExp;
import fr.wonder.ahk.compiled.expressions.ConversionExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.FunctionCallExp;
import fr.wonder.ahk.compiled.expressions.FunctionExp;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.compiler.types.Operation;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnimplementedException;

class ExpressionLinker {

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
						componentsType = typesTable.conversions.getCommonParent(componentsType, values[j].getType());
						if(componentsType == null)
							break;
					}
					if(componentsType == null) {
						errors.add("No shared type in array declaration" + array.getErr());
						componentsType = Invalids.TYPE;
					}
					array.type = new VarArrayType(componentsType);
				}
				
			} else if(exp instanceof FunctionCallExp) {
				FunctionCallExp fexp = (FunctionCallExp) exp;
				if(fexp.getFunction() instanceof VarExp) {
					// replace the FunctionCallExp by a FunctionExp
					FunctionPrototype function = searchMatchingFunction(scope.getUnitScope(), fexp, typesTable, errors);
					if(function != null) {
						if(!function.getSignature().declaringUnit.equals(unit.fullBase))
							unit.prototype.externalAccesses.add(function);
						FunctionExp functionExpression = new FunctionExp(unit.source, fexp, function);
						exp = expressions[i] = functionExpression;
						for(int j = 0; j < fexp.getArguments().length; j++) {
							Expression argument = functionExpression.getArguments()[j];
							if(function.functionType.arguments[j] != argument.getType()) {
								VarType converted = function.functionType.arguments[j];
								ConversionExp conv = new ConversionExp(unit.source, argument, converted, true);
								functionExpression.expressions[j] = conv;
							}
						}
					} else {
						errors.dump();
						// TODO implement functions as variables
						throw new UnimplementedException("No matching function " + fexp.getErr());
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
			}
			
			exp.computeValueType(typesTable, errors);
		}
	}

	private static FunctionPrototype searchMatchingFunction(UnitScope unitScope, FunctionCallExp fexp,
			TypesTable typesTable, ErrorWrapper errors) {
		String funcName = ((VarExp) fexp.getFunction()).variable;
		VarType[] argumentsTypes = fexp.getArgumentsTypes();
		{ // search without casting
			FunctionPrototype function = unitScope.getFunctionStrict(
					funcName, argumentsTypes);
			if(function != null)
				return function;
		}
		{ // search with a single cast
			int singleConversionCount = unitScope.countMatchingFunction1c(
					funcName, argumentsTypes, typesTable.conversions);
			if(singleConversionCount == 1)
				return unitScope.getFunction1c(funcName, argumentsTypes, typesTable.conversions);
			if(singleConversionCount > 1) {
				errors.add("Multiple matching functions " + fexp.getErr());
				return null;
			}
		}
		{ // search with any number of casts
			int anyConversionCount = unitScope.countMatchingFunctionXc(
					funcName, argumentsTypes, typesTable.conversions);
			if(anyConversionCount == 1)
				return unitScope.getFunctionXc(funcName, argumentsTypes, typesTable.conversions);
			if(anyConversionCount > 1) {
				errors.add("Multiple matching functions " + fexp.getErr());
				return null;
			}
			return null;
		}
	}

}
