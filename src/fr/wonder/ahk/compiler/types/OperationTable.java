package fr.wonder.ahk.compiler.types;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.commons.types.Tuple;

public class OperationTable {
	
	public static final VarType TYPE_ANY = null;
	private static final Map<VarType, Operation> emptyMap = Collections.emptyMap();
	private static final Map<Tuple<VarType, Operator>, Map<VarType, Operation>> operations = new HashMap<>();
	
	public void registerOperation(VarType lOperand, Operator operator, VarType rOperand, Operation operation) {
		Operation overriden = operations.computeIfAbsent(new Tuple<>(lOperand, operator), x -> new HashMap<>()).put(rOperand, operation);
		if(overriden != null)
			throw new IllegalStateException("An operation was overriden: " + lOperand + operator + rOperand);
	}
	
	public Operation getOperation(VarType lOperand, Operator operator, VarType rOperand) {
		return operations.getOrDefault(new Tuple<>(lOperand, operator), emptyMap).get(rOperand);
	}
	
}
