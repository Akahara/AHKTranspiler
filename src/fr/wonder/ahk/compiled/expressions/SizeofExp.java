package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class SizeofExp extends Expression {

	public SizeofExp(SourceReference sourceRef, Expression exp) {
		super(sourceRef, exp);
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
