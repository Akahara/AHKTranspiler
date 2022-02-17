package fr.wonder.ahk.compiler.types;

import java.util.HashMap;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarNativeType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.types.Triplet;

public class TypesTable {
	
	private final Map<Triplet<VarType, VarType, Operator>, OverloadedOperatorPrototype> operations = new HashMap<>();
	private final Map<Triplet<VarType, VarType, Operator>, Operation> functionOperations = new HashMap<>();
	
	public TypesTable() {
		
	}
	
	private static class OperationQuery {
		
		private final VarType lo, ro;
		private final Operator operator;
		private final SourceElement queryElement;
		
		private OperationQuery(VarType lo, VarType ro, Operator operator, SourceElement queryElement) {
			this.lo = lo;
			this.ro = ro;
			this.operator = operator;
			this.queryElement = queryElement;
		}
		
		private String fullErr() {
			return lo.toString() + " " + operator + " " + ro.toString() + ":" + queryElement.getErr();
		}
		
	}
	
	public Operation getOperation(VarType leftOp, VarType rightOp, Operator operator, SourceElement queryElement, ErrorWrapper errors) {
		return getOperation(new OperationQuery(leftOp, rightOp, operator, queryElement), errors);
	}
	
	private Operation getOperation(OperationQuery query, ErrorWrapper errors) {
		// only the left operand may be null
		
		// if both operands are primitives, only search through native operations
		if((query.lo instanceof VarNativeType || query.lo == null) && query.ro instanceof VarNativeType) {
			Operation op = NativeOperation.getOperation((VarNativeType) query.lo, (VarNativeType) query.ro, query.operator, true);
			if(op == null)
				errors.add("Unimplemented native operation " + query.fullErr());
			return op;
		}
		
		// if at least one operand is a struct, search through overloaded query.operators
		// if none match keep searching
		if(query.lo instanceof VarStructType || query.ro instanceof VarStructType) {
			Operation op = getKnownOperation(query);
			if(op != null)
				return op;
		}
		
		// if at least one operand is a function, check if the query.operator can be applied
		if(query.lo instanceof VarFunctionType || query.ro instanceof VarFunctionType)
			return getFunctionOperation(query, errors);
		
		errors.add("No operation exists between types " + query.fullErr());
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
	
	private Operation getKnownOperation(OperationQuery query) {
		return operations.get(opKey(query.lo, query.ro, query.operator));
	}
	
	private Operation getFunctionOperation(OperationQuery query, ErrorWrapper errors) {
		Operator operator = query.operator;
		Operation known = functionOperations.get(opKey(query.lo, query.ro, operator));
		if(known != null)
			return known;
		
		if(operator == Operator.SHL || operator == Operator.SHR)
			return getCompositionOperation(query, errors);
		else
			return getFunctionOperatorOperation(query, errors);
	}

	private Operation getFunctionOperatorOperation(OperationQuery query, ErrorWrapper errors) {
		
		Operation resultOp;
		VarType[] funcTypeArgs;
		
		if(query.lo instanceof VarFunctionType && query.ro instanceof VarFunctionType) {
			// func + func
			VarFunctionType f1 = (VarFunctionType) query.lo;
			VarFunctionType f2 = (VarFunctionType) query.ro;
			
			if(!FunctionArguments.matchNoConversions(f1.arguments, f2.arguments)) {
				errors.add("Incompatible function types: (" + Utils.toString(f1.arguments) +
						") and (" + Utils.toString(f2.arguments) + ")" + query.queryElement.getErr());
				return null;
			}
			
			resultOp = getOperation(new OperationQuery(f1.returnType, f2.returnType, query.operator, query.queryElement), errors);
			if(resultOp != null && 
					(!resultOp.loType.equals(f1.returnType) ||
					 !resultOp.roType.equals(f2.returnType))) {
				errors.add("Result types " + f1.returnType + " are incompatible for operation " +
					 query.operator + query.queryElement.getErr());
				return null; // TODO support casts right before closure operations
							 // (func float() + func int() = func float())
							 // also remove the same checks below vvv
			}
			funcTypeArgs = f1.arguments;
			
		} else if(query.lo instanceof VarFunctionType) {
			// func + obj
			VarFunctionType f = (VarFunctionType) query.lo;
			resultOp = getOperation(new OperationQuery(f.returnType, query.ro, query.operator, query.queryElement), errors);
			funcTypeArgs = f.arguments;
			if(resultOp != null && !resultOp.loType.equals(f.returnType)) {
				errors.add("Invalid constant type " + resultOp.loType + 
						" is incompatible with type " + f.returnType +
						" for operation " + query.operator +
						query.queryElement.getErr());
				return null;
			}
		
		} else if(query.ro instanceof VarFunctionType) {
			// obj + func
			VarFunctionType f = (VarFunctionType) query.ro;
			resultOp = getOperation(new OperationQuery(query.lo, f.returnType, query.operator, query.queryElement), errors);
			funcTypeArgs = f.arguments;
			if(resultOp != null && !resultOp.roType.equals(f.returnType)) {
				errors.add("Invalid constant type " + resultOp.roType + 
						" is incompatible with type " + f.returnType +
						" for operation " + query.operator +
						query.queryElement.getErr());
				return null;
			}
			
		} else {
			throw new IllegalArgumentException("Not function types");
		}
		
		if(resultOp == null) {
			errors.add("No known function operation for " + query.fullErr());
			return null;
		}
		VarFunctionType funcType = new VarFunctionType(resultOp.resultType, funcTypeArgs);
		FunctionOperation funcOperation = new FunctionOperation(query.lo, query.ro, resultOp, funcType);
		functionOperations.put(opKey(query.lo, query.ro, query.operator), funcOperation);
		return funcOperation;
	}
	
	private Operation getCompositionOperation(OperationQuery query, ErrorWrapper errors) {
		if(!(query.lo instanceof VarFunctionType) || ! (query.ro instanceof VarFunctionType)) {
			errors.add("Invalid composition operation " + query.fullErr());
			return null;
		}
		
		VarFunctionType f1;
		VarFunctionType f2;
		
		if(query.operator == Operator.SHL) {
			f1 = (VarFunctionType) query.lo;
			f2 = (VarFunctionType) query.ro;
		} else {
			f1 = (VarFunctionType) query.ro;
			f2 = (VarFunctionType) query.lo;
		}
		
		if(f2.arguments.length != 1 || !f1.returnType.equals(f2.arguments[0])) {
			errors.add("Invalid function composition " + f1.getSignature() + " and " +
					f2.getSignature() + " " + query.queryElement.getErr());
			return null;
		}

		CompositionOperation composed = new CompositionOperation(f1, f2, Operator.SHR);
		CompositionOperation composedR = new CompositionOperation(f2, f1, Operator.SHL);
		functionOperations.put(opKey(composed), composed);
		functionOperations.put(opKey(composedR), composedR);
		
		return query.operator == Operator.SHR ? composed : composedR;
	}
	
}
