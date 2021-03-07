package fr.wonder.ahk.compiler.types;

import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarNativeType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;

public class TypesTable {
	
	public final OperationTable operations = new OperationTable();
	public final ConversionTable conversions = new ConversionTable();
	
	public TypesTable() {
		
	}
	
	public VarType getOperationResult(OperationExp exp) {
		Operation op = getOperation(exp);
		return op == null ? null : op.getResultType();
	}
	
	public Operation getOperation(OperationExp exp) {
		// only the left operand may be null
		VarType leftOp = exp.getLOType();
		VarType rightOp = exp.getROType();
		Operator operator = exp.operator;
		
		if(leftOp instanceof VarArrayType || rightOp instanceof VarArrayType)
			return null;
		
		if(leftOp instanceof VarNativeType && leftOp instanceof VarNativeType)
			return NativeOperation.getOperation(leftOp, operator, rightOp);
		
		if(leftOp instanceof VarStructType) {
			// check if this operation was already mapped
			Operation operation = operations.getOperation(leftOp, operator, OperationTable.TYPE_ANY);
			if(operation != null)
				return operation;
			// else retrieve the operation
			VarType original = leftOp;
			do {
				// if no operation matches, check for super type
				leftOp = ((VarStructType) leftOp).superType;
				operation = operations.getOperation(leftOp, operator, OperationTable.TYPE_ANY);
			} while (operation == null && leftOp != null);
			if(operation != null) {
				// map the found operation for future queries
				while(true) {
					operations.registerOperation(original, operator, OperationTable.TYPE_ANY, operation);
					original = ((VarStructType) original).superType;
					if(original == leftOp)
						break;
				}
				return operation;
			}
		}
		
		if(leftOp == null && rightOp instanceof VarStructType) {
			// TODO check if - <struct> is interpreted as the negative operation or throws an expression error
//			// check if this operation was already mapped
//			Operation operation = operations.getOperation(null, operator, rightOp);
//			if(operation != null)
//				return operation;
//			// else retrieve the operation
//			VarType original = rightOp;
//			do {
//				// if no operation matches, check for super type
//				rightOp = ((VarStructType) rightOp).superType;
//				operation = operations.getOperation(null, operator, rightOp);
//			} while (operation == null && rightOp != null);
//			if(operation != null) {
//				// map the found operation for future queries
//				while(true) {
//					operations.addOperation(null, operator, original, operation);
//					original = ((VarStructType) original).superType;
//					if(original == leftOp)
//						break;
//				}
//				return operation;
//			}
		}
		
		return null;
	}
	
}
