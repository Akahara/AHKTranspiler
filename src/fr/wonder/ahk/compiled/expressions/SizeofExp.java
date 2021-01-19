package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class SizeofExp extends Expression {

	public SizeofExp(UnitSource source, int sourceStart, int sourceStop, Expression exp) {
		super(source, sourceStart, sourceStop, exp);
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
