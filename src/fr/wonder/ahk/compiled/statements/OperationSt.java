package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.units.SourceReference;

public class OperationSt extends Statement {

	public OperationSt(SourceReference sourceRef, OperationExp operation) {
		super(sourceRef, operation);
	}
	
	public OperationExp getOperation() {
		return (OperationExp) expressions[0];
	}

	@Override
	public String toString() {
		return getOperation().toString();
	}

}
