package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.linker.Signatures;
import fr.wonder.ahk.compiler.types.Operation;

public class OverloadedOperatorPrototype extends Operation implements Prototype<OverloadedOperatorPrototype> {
	
	public final Signature signature;
	
	/** Set by the linker */
	public FunctionPrototype function;
	
	public OverloadedOperatorPrototype(StructSection structure, Operator operator,
			VarType leftOperand, VarType rightOperand, VarType resultType) {
		super(leftOperand, rightOperand, operator, resultType);
		this.signature = Signatures.of(this, structure);
	}
	
	@Override
	public Signature getSignature() {
		return signature;
	}

}
