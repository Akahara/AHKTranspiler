package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;

public class BlueprintOperator implements SourceElement {
	
	public final SourceReference sourceRef;
	
	public final VarType leftOperand, rightOperand;
	public final Operator operator;
	public final VarType resultType;
	
	/** Set by the linker using {@link #setSignature(Signature)} */
	public OverloadedOperatorPrototype prototype;
	
	public BlueprintOperator(SourceReference sourceRef, VarType leftOperand,
			VarType rightOperand, Operator operator, VarType resultType) {
		this.sourceRef = sourceRef;
		this.leftOperand = leftOperand;
		this.rightOperand = rightOperand;
		this.operator = operator;
		this.resultType = resultType;
	}

	@Override
	public SourceReference getSourceReference() {
		return sourceRef;
	}
	
	public void setSignature(Signature signature) {
		this.prototype = new OverloadedOperatorPrototype(
				operator, leftOperand, rightOperand, resultType, signature);
	}
	
	public OverloadedOperatorPrototype getPrototype() {
		return prototype;
	}

}
