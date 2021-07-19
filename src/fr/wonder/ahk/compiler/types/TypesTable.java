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
		
		if((leftOp instanceof VarNativeType || leftOp == null) && rightOp instanceof VarNativeType)
			return NativeOperation.getOperation(leftOp, rightOp, operator, true);
		
		if(leftOp instanceof VarStructType || rightOp instanceof VarStructType)
			return operations.getOperation(leftOp, operator, OperationTable.TYPE_ANY);
		
		return null;
	}
	
}
