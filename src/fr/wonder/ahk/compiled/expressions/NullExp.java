package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class NullExp extends Expression {
	
	public VarType type = VarType.NULL; // FIX set the type via the linker, the null type does NOT represent the type of this expression
	
	public NullExp(Unit unit, int sourceStart, int sourceStop) {
		super(unit, sourceStart, sourceStop);
	}
	
	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		return type;
	}

	@Override
	public String toString() {
		return "NULL";
	}

}
