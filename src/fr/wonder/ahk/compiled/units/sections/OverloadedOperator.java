package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceObject;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;

public class OverloadedOperator extends SourceObject {

	public final OverloadedOperatorPrototype prototype;
	public final String functionName;
	
	public OverloadedOperator(StructSection structure, Operator op, VarType resultType,
			VarType leftOperand, VarType rightOperand, String functionName,
			int sourceStart, int sourceStop) {
		
		super(structure.unit.source, sourceStart, sourceStop);
		this.prototype = new OverloadedOperatorPrototype(
				structure, op,
				leftOperand, rightOperand, resultType);
		this.functionName = functionName;
	}
	
	public OverloadedOperatorPrototype getPrototype() {
		return prototype;
	}
	
}
