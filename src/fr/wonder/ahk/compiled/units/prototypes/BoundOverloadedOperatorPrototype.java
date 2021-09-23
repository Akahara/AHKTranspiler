package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.types.VarGenericType;
import fr.wonder.ahk.compiled.expressions.types.VarType;

public class BoundOverloadedOperatorPrototype extends OverloadedOperatorPrototype {

	public final VarGenericType genericType;
	public final BlueprintPrototype usedBlueprint;
	public final OverloadedOperatorPrototype usedOperator;
	
	public BoundOverloadedOperatorPrototype(
			VarType leftOperand,
			VarType rightOperand,
			VarType resultType,
			VarGenericType originalGeneric,
			BlueprintPrototype usedBlueprint,
			OverloadedOperatorPrototype originalOperator) {
		super(	originalOperator.operator,
				leftOperand,
				rightOperand,
				resultType,
				originalOperator.signature);
		this.genericType = originalGeneric;
		this.usedBlueprint = usedBlueprint;
		this.usedOperator = originalOperator;
	}
}
