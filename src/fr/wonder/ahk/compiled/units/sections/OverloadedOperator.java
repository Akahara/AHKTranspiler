package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceObject;
import fr.wonder.ahk.compiled.units.Unit;

public class OverloadedOperator extends SourceObject {

	public final Operator operator;
	public final VarType returnType, leftOperand, rightOperand;
	
	public OverloadedOperator(Unit unit, Operator op, VarType returnType,
			VarType leftOperand, VarType rightOperand,
			int sourceStart, int sourceStop) {
		
		super(unit.source, sourceStart, sourceStop);
		this.operator = op;
		this.returnType = returnType;
		this.leftOperand = leftOperand;
		this.rightOperand = rightOperand;
	}
	
	
	
}
