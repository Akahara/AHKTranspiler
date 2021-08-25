package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;

public class OverloadedOperator implements SourceElement {

	public final SourceReference sourceRef;
	public final OverloadedOperatorPrototype prototype;
	public final String functionName;
	
	public OverloadedOperator(StructSection structure, SourceReference sourceRef, Operator op, VarType resultType,
			VarType leftOperand, VarType rightOperand, String functionName) {
		this.sourceRef = sourceRef;
		this.prototype = new OverloadedOperatorPrototype(
				structure, op,
				leftOperand, rightOperand, resultType);
		this.functionName = functionName;
	}
	
	@Override
	public SourceReference getSourceReference() {
		return sourceRef;
	}
	
	public OverloadedOperatorPrototype getPrototype() {
		return prototype;
	}
	
}
