package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class SizeofExp extends Expression {

	public SizeofExp(Unit unit, int sourceStart, int sourceStop, Expression exp) {
		super(unit, sourceStart, sourceStop, exp);
	}
	
	public Expression getExpression() {
		return expressions[0];
	}
	
	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		return VarType.INT;
	}

	@Override
	public String toString() {
		return "sizeof(" + getExpression() + ")";
	}
	
}
