package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.expressions.ArrayExp;
import fr.wonder.ahk.compiled.expressions.ConstructorExp;
import fr.wonder.ahk.compiled.expressions.ConversionExp;
import fr.wonder.ahk.compiled.expressions.DirectAccessExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.FunctionCallExp;
import fr.wonder.ahk.compiled.expressions.FunctionExp;
import fr.wonder.ahk.compiled.expressions.FunctionExpression;
import fr.wonder.ahk.compiled.expressions.IndexingExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.NullExp;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.SimpleLambdaExp;
import fr.wonder.ahk.compiled.expressions.SizeofExp;
import fr.wonder.ahk.compiled.expressions.UninitializedArrayExp;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.Prototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.types.ConversionTable;
import fr.wonder.ahk.compiler.types.FunctionArguments;
import fr.wonder.ahk.compiler.types.Operation;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnreachableException;
import fr.wonder.commons.utils.ArrayOperator;
import fr.wonder.commons.utils.Assertions;

class ExpressionLinker {

	private final Linker linker;
	
	ExpressionLinker(Linker linker) {
		this.linker = linker;
	}
	
	void linkExpressions(Unit unit, Scope scope, ExpressionHolder expressionHolder, ErrorWrapper errors) {
		
		Expression[] expressions = expressionHolder.getExpressions();
		for(int i = 0; i < expressions.length; i++) {
			Expression exp = expressions[i];
			
			if(exp == null)
				continue;
			
			linkExpressions(unit, scope, exp, errors);
			
			if(exp instanceof VarExp) {
				linkVariableExpression(unit, scope, (VarExp) exp, errors);
				
			} else if(exp instanceof ArrayExp) {
				linkArrayExpression((ArrayExp) exp, errors);
				
			} else if(exp instanceof UninitializedArrayExp) {
				linkUninitializedArrayExp((UninitializedArrayExp) exp, errors);
				
			} else if(exp instanceof IndexingExp) {
				linkIndexingExpression((IndexingExp) exp, errors);
				
			} else if(exp instanceof FunctionCallExp) {
				exp = linkFunctionExpression(unit, (FunctionCallExp) exp, errors);
				expressions[i] = exp;
				
			} else if(exp instanceof OperationExp) {
				linkOperationExpression(unit, (OperationExp) exp, errors);
			
			} else if(exp instanceof ConstructorExp) {
				linkConstructorExpression(unit, (ConstructorExp) exp, errors);
				
			} else if(exp instanceof DirectAccessExp) {
				linkDirectAccessExp(unit, (DirectAccessExp) exp, errors);
				
			} else if(exp instanceof ConversionExp) {
				linkConversionExp((ConversionExp) exp, errors);
				
			} else if(exp instanceof SizeofExp) {
				linkSizeofExp((SizeofExp) exp, errors);
				
			} else if(exp instanceof SimpleLambdaExp) {
				linkSimpleLambdaExp(unit, scope, (SimpleLambdaExp) exp, errors);
				
			} else if(exp instanceof NullExp || exp instanceof LiteralExp<?>) {
				// pass
				
			} else {
				throw new UnreachableException("Unimplemented expression type: " + exp.getClass());
				
			}
			
			Assertions.assertNonNull("An expression was not given its type: " + exp.getClass(), exp.getType());
			linker.requireType(unit, exp.getType(), exp, errors);
		}
	}

	private void linkVariableExpression(Unit unit, Scope scope, VarExp exp, ErrorWrapper errors) {
		// search for the variable/function declaration
		VarAccess var = scope.getVariable(exp.variable, exp, errors);
		
		if(!var.getSignature().declaringUnit.equals(unit.fullBase) && var.getSignature().declaringUnit != VarAccess.INNER_UNIT) {
			Prototype<?> proto = (Prototype<?>) var;
			unit.prototype.externalAccesses.add(proto);
		}
		exp.declaration = var;
		exp.type = var.getType();
	}

	private void linkArrayExpression(ArrayExp exp, ErrorWrapper errors) {
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
	
	private void linkUninitializedArrayExp(UninitializedArrayExp exp, ErrorWrapper errors) {
		Expression size = exp.getSize();
		if(size.getType() != VarType.INT) {
			errors.add("Arrays must be initialized with an integer size, "
					+ size.getType().getName() + " was provided" + exp.getErr());
		}
		exp.type = VarArrayType.EMPTY_ARRAY;
	}

	private void linkIndexingExpression(IndexingExp exp, ErrorWrapper errors) {
		// validate types, there is no linking to do
		VarType arrayType = exp.getArray().getType();
		Expression[] indices = exp.getIndices();
		
		VarType expType = arrayType;
		for(Expression index : indices) {
			if(index.getType() != VarType.INT)
				errors.add("An index must have type int" + index.getErr());
			if(expType != Invalids.TYPE) {
				if(expType instanceof VarArrayType) {
					expType = ((VarArrayType) expType).componentType;
				} else {
					errors.add("Type " + expType + " cannot be indexed " + exp.getErr());
					expType = Invalids.TYPE;
				}
			}
		}
		exp.type = expType;
	}

	private void linkConstructorExpression(Unit unit, ConstructorExp exp, ErrorWrapper errors) {
		exp.constructor = Invalids.CONSTRUCTOR_PROTOTYPE;
		exp.type = Invalids.STRUCT_TYPE;
		
		VarType baseType = exp.constructorType;
		if(!(baseType instanceof VarStructType)) {
			errors.add("Invalid constructor type: " + baseType + exp.getErr());
			return;
		}
		StructPrototype structure = ((VarStructType) baseType).structure;
		
		ConstructorPrototype[] accessibleConstructors = structure.constructors;
		
		VarType[] args = ArrayOperator.map(exp.getExpressions(), VarType[]::new, Expression::getType);
		if(!unit.fullBase.equals(structure.signature.declaringUnit))
			accessibleConstructors = ArrayOperator.filter(accessibleConstructors, c -> c.modifiers.visibility == DeclarationVisibility.GLOBAL);
		ConstructorPrototype matchingConstructor = FunctionArguments.searchMatchingConstructor(accessibleConstructors, args, exp, errors);
		
		if(matchingConstructor == null) {
			exp.constructor = Invalids.CONSTRUCTOR_PROTOTYPE;
			exp.type = Invalids.STRUCT_TYPE;
		} else {
			exp.constructor = matchingConstructor;
			exp.type = baseType;
		}
	}

	private void linkOperationExpression(Unit unit, OperationExp exp, ErrorWrapper errors) {
		
		Operation op = linker.typesTable.getOperation(
				exp.getLOType(), exp.getROType(),
				exp.operator, exp, errors);
		
		if(op == null) {
			errors.add("Invalid operation! " + exp.operationString() + exp.getErr());
			op = Invalids.OPERATION;
		} else if(op instanceof OverloadedOperatorPrototype) {
			OverloadedOperatorPrototype oop = (OverloadedOperatorPrototype) op;
			unit.prototype.externalAccesses.add(oop);
		}
		
		// #setOperation replaces the operation expression operands by cast expressions if needed
		exp.setOperation(op);
		exp.type = op.resultType;
	}

	private void linkDirectAccessExp(Unit unit, DirectAccessExp exp, ErrorWrapper errors) {
		VarType instanceType = exp.getStruct().getType();
		if(!(instanceType instanceof VarStructType)) {
			errors.add("Cannot access a member of an instance of type " + instanceType + exp.getErr());
			exp.member = Invalids.VARIABLE_PROTO;
			exp.type = Invalids.TYPE;
			return;
		}
		StructPrototype prototype = ((VarStructType) instanceType).structure;
		VariablePrototype member = prototype.getMember(exp.memberName);
		VarType memberType = member == null ? null : member.type;
		
		exp.member = Invalids.VARIABLE_PROTO;
		exp.type = Invalids.TYPE;
		
		if(member == null) {
			// no member with given name
			errors.add("Type " + prototype.getName() + " does not have a member named " + exp.memberName + exp.getErr());
		
		} else if(!prototype.signature.declaringUnit.equals(unit.fullBase) &&
				member.modifiers.visibility == DeclarationVisibility.LOCAL) {
			// member is not accessible
			errors.add("Member " + exp.memberName + " of structure " + prototype.getName()
					+ " is not accessible:" + exp.getErr());
			
		} else if(memberType instanceof VarStructType && 
				!linker.requireType(unit, memberType, exp, errors)) {
			// check if member can be accessed (may not be because of non-imported struct type)
			errors.add("Member " + exp.memberName + " of structure " + prototype.getName()
					+ " has type " + memberType + " which cannot be accessed" + exp.getErr());
			
		} else {
			// can access member
			exp.member = member;
			exp.type = memberType;
		}
		
		if(exp.type == null)
			throw null;
	}

	private Expression linkFunctionExpression(Unit unit, FunctionCallExp fexp, ErrorWrapper errors) {
		
		FunctionExpression finalFunction;
		VarFunctionType functionType;
		// if possible, replace the FunctionCallExp by a FunctionExp
		if(fexp.getFunction() instanceof VarExp && ((VarExp) fexp.getFunction()).declaration instanceof FunctionPrototype) {
			FunctionPrototype function = (FunctionPrototype) ((VarExp) fexp.getFunction()).declaration;
			FunctionExp functionExpression = new FunctionExp(fexp, function);
			finalFunction = functionExpression;
			functionType = function.functionType;
		} else if(fexp.getFunction().getType() instanceof VarFunctionType) {
			functionType = (VarFunctionType) fexp.getFunction().getType();
			finalFunction = fexp;
			fexp.functionType = functionType;
		} else {
			// if invalid, an error was already reported, do not add another
			if(fexp.getFunction().getType() != Invalids.TYPE)
				errors.add("Type " + fexp.getFunction().getType() + " is not callable:" + fexp.getErr());
			fexp.functionType = Invalids.FUNCTION_TYPE;
			fexp.type = Invalids.TYPE;
			return fexp;
		}
		if(finalFunction.argumentCount() != functionType.arguments.length) {
			errors.add("Invalid number of arguments: function takes " 
					+ functionType.arguments.length + " but " 
					+ finalFunction.argumentCount() 
					+ " are given:" + finalFunction.getErr());
		} else {
			for(int j = 0; j < functionType.arguments.length; j++) {
				linker.checkAffectationType(finalFunction, j, functionType.arguments[j], errors);
			}
		}
		
		finalFunction.type = functionType.returnType;
		return finalFunction;
	}
	
	private void linkConversionExp(ConversionExp exp, ErrorWrapper errors) {
		VarType origin = exp.getValue().getType();
		VarType cast = exp.castType;
		if(exp.isImplicit ?
				!ConversionTable.canConvertImplicitly(origin, cast) :
				!ConversionTable.canConvertExplicitly(origin, cast)) {
			errors.add("Unable to convert explicitely from type " + origin + " to " + cast);
			exp.type = Invalids.TYPE;
		} else {
			exp.type = cast;
		}
	}
	
	private void linkSizeofExp(SizeofExp exp, ErrorWrapper errors) {
		VarType mesured = exp.getExpression().getType();
		if(!SizeofExp.isMesurableType(mesured))
			errors.add("Type " + mesured + " is not mesurable:" + exp.getErr());
	}
	
	private void linkSimpleLambdaExp(Unit unit, Scope currentScope, SimpleLambdaExp exp, ErrorWrapper errors) {
		linker.linkLambda(unit, currentScope, exp.lambda, errors);
		exp.type = exp.lambda.lambdaFunctionType;
	}

}
