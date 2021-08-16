package fr.wonder.ahk.compiler.types;

import java.util.HashMap;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.commons.types.Triplet;

public class OperationTable {
	
	private static final Map<Triplet<VarType, VarType, Operator>, OverloadedOperatorPrototype> operations = new HashMap<>();

	private static Triplet<VarType, VarType, Operator> opKey(Operation o) {
		return opKey(o.loType, o.roType, o.operator);
	}
	private static Triplet<VarType, VarType, Operator> opKey(VarType lOperand, VarType rOperand, Operator operator) {
		return new Triplet<>(lOperand, rOperand, operator);
	}
	
	/** Returns the overridden operation (if any) */
	public OverloadedOperatorPrototype registerOperation(OverloadedOperatorPrototype operation) {
		return operations.put(opKey(operation), operation);
	}
	
	public Operation getOperation(VarType lOperand, VarType rOperand, Operator operator) {
		return operations.get(opKey(lOperand, rOperand, operator));
	}
	
}
