package fr.wonder.ahk.compiler.types;

import java.util.HashMap;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarNativeType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.commons.types.Triplet;

public class TypesTable {
	
	private final Map<Triplet<VarType, VarType, Operator>, OverloadedOperatorPrototype> operations = new HashMap<>();
	private final Map<Triplet<VarType, VarType, Operator>, Operation> functionOperations = new HashMap<>();
	
	public TypesTable() {
		
	}
	
	public Operation getOperation(OperationExp exp) {
		return getOperation(exp.getLOType(), exp.getROType(), exp.operator);
	}

	public Operation getOperation(VarType leftOp, VarType rightOp, Operator operator) {
		// only the left operand may be null
		
		// if both operands are primitives, only search through native operations
		if((leftOp instanceof VarNativeType || leftOp == null) && rightOp instanceof VarNativeType)
			return NativeOperation.getOperation(leftOp, rightOp, operator, true);
		
		// if at least one operand is a struct, search through overloaded operators
		// if none match keep searching
		if(leftOp instanceof VarStructType || rightOp instanceof VarStructType) {
			Operation op = getKnownOperation(leftOp, rightOp, operator);
			if(op != null)
				return op;
		}
		
		// if at least one operand is a function, check if the operator can be applied
		if(leftOp instanceof VarFunctionType || rightOp instanceof VarFunctionType)
			return getFunctionOperation(leftOp, rightOp, operator);
		
		return null;
	}
	
	private static Triplet<VarType, VarType, Operator> opKey(Operation o) {
		return opKey(o.loType, o.roType, o.operator);
	}
	private static Triplet<VarType, VarType, Operator> opKey(VarType lOperand, VarType rOperand, Operator operator) {
		return new Triplet<>(lOperand, rOperand, operator);
	}
	
	/** Returns the overridden operation (if any) */
	public OverloadedOperatorPrototype registerOverloadedOperator(OverloadedOperatorPrototype operation) {
		return operations.put(opKey(operation), operation);
	}
	
	private Operation getKnownOperation(VarType lOperand, VarType rOperand, Operator operator) {
		return operations.get(opKey(lOperand, rOperand, operator));
	}
	
	private Operation getFunctionOperation(VarType leftOp, VarType rightOp, Operator operator) {
		Operation known = functionOperations.get(opKey(leftOp, rightOp, operator));
		if(known != null)
			return known;
		
		if(operator == Operator.SHL || operator == Operator.SHR)
			return getCompositionOperation(leftOp, rightOp, operator);
		else
			return getFunctionOperatorOperation(leftOp, rightOp, operator);
	}

	private Operation getFunctionOperatorOperation(VarType leftOp, VarType rightOp, Operator operator) {
		Operation resultOp;
		VarType[] funcTypeArgs;
		
		if(leftOp instanceof VarFunctionType && rightOp instanceof VarFunctionType) {
			// func + func
			VarFunctionType f1 = (VarFunctionType) leftOp;
			VarFunctionType f2 = (VarFunctionType) rightOp;
			
			if(!FunctionArguments.matchNoConversions(f1.arguments, f2.arguments))
				return null;
			
			resultOp = getOperation(f1.returnType, f2.returnType, operator);
			if(resultOp != null && 
					(!resultOp.loType.equals(f1.returnType) ||
					 !resultOp.roType.equals(f2.returnType)))
				return null; // TODO support casts right before closure operations
							 // (func float() + func int() = func float())
							 // also remove the same checks below vvv
			funcTypeArgs = f1.arguments;
			
		} else if(leftOp instanceof VarFunctionType) {
			// func + obj
			VarFunctionType f = (VarFunctionType) leftOp;
			resultOp = getOperation(f.returnType, rightOp, operator);
			funcTypeArgs = f.arguments;
			if(resultOp != null && !resultOp.loType.equals(f.returnType))
				return null;
		
		} else if(rightOp instanceof VarFunctionType) {
			// obj + func
			VarFunctionType f = (VarFunctionType) rightOp;
			resultOp = getOperation(leftOp, f.returnType, operator);
			funcTypeArgs = f.arguments;
			if(resultOp != null && !resultOp.roType.equals(f.returnType))
				return null;
			
		} else {
			throw new IllegalArgumentException("Not function types");
		}
		
		if(resultOp == null)
			return null;
		VarFunctionType funcType = new VarFunctionType(resultOp.resultType, funcTypeArgs);
		FunctionOperation funcOperation = new FunctionOperation(leftOp, rightOp, resultOp, funcType);
		functionOperations.put(opKey(leftOp, rightOp, operator), funcOperation);
		return funcOperation;
	}
	
	private Operation getCompositionOperation(VarType leftOp, VarType rightOp, Operator operator) {
		if(!(leftOp instanceof VarFunctionType) || ! (rightOp instanceof VarFunctionType))
			return null;
		
		VarFunctionType f1;
		VarFunctionType f2;
		
		if(operator == Operator.SHL) {
			f1 = (VarFunctionType) leftOp;
			f2 = (VarFunctionType) rightOp;
		} else {
			f1 = (VarFunctionType) rightOp;
			f2 = (VarFunctionType) leftOp;
		}
		
		if(!FunctionArguments.matchNoConversions(new VarType[] { f1.returnType }, f2.arguments))
			return null;

		CompositionOperation composed = new CompositionOperation(f1, f2, Operator.SHR);
		CompositionOperation composedR = new CompositionOperation(f2, f1, Operator.SHL);
		functionOperations.put(opKey(composed), composed);
		functionOperations.put(opKey(composedR), composedR);
		
		return operator == Operator.SHR ? composed : composedR;
	}
	
}
