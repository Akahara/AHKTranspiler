package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceObject;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiler.types.Operation;

public class OverloadedOperator extends SourceObject implements Operation {

	public final Operator operator;
	public final VarType resultType, leftOperand, rightOperand;
	public final String functionName;
	
	/** Set by the linker */
	public FunctionPrototype function;
	
	public OverloadedOperator(Unit unit, Operator op, VarType resultType,
			VarType leftOperand, VarType rightOperand, String functionName,
			int sourceStart, int sourceStop) {
		
		super(unit.source, sourceStart, sourceStop);
		this.operator = op;
		this.resultType = resultType;
		this.leftOperand = leftOperand;
		this.rightOperand = rightOperand;
		this.functionName = functionName;
	}

	@Override
	public VarType getResultType() {
		return resultType;
	}

	@Override
	public VarType getLOType() {
		return leftOperand;
	}

	@Override
	public VarType getROType() {
		return rightOperand;
	}
	
	public int argCount() {
		return getLOType() == null ? 1 : 2;
	}
	
}
