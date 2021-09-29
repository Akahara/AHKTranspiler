package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.linker.Signatures;
import fr.wonder.ahk.compiler.types.Operation;
import fr.wonder.commons.annotations.Nullable;
import fr.wonder.commons.utils.Assertions;

public class OverloadedOperatorPrototype extends Operation implements Prototype<OverloadedOperatorPrototype> {
	
	public final Signature signature;
	
	/**
	 * Set by the linker<br>
	 * May be null if this instance is of type {@link BoundOverloadedOperatorPrototype},
	 * in which case the operation can be retrieved by the operator's type GIP (see generic doc).
	 */
	@Nullable
	public FunctionPrototype function;

	public OverloadedOperatorPrototype(Operator operator, VarType leftOperand,
			VarType rightOperand, VarType resultType, Signature signature) {
		super(leftOperand, rightOperand, operator, resultType);
		this.signature = signature;
		Assertions.assertNonNull(signature);
	}
	
	public OverloadedOperatorPrototype(StructSection structure, Operator operator, VarType leftOperand,
			VarType rightOperand, VarType resultType) {
		super(leftOperand, rightOperand, operator, resultType);
		this.signature = Signatures.of(this, structure);
		Assertions.assertNonNull(signature);
	}
	
	@Override
	public Signature getSignature() {
		return signature;
	}

}
