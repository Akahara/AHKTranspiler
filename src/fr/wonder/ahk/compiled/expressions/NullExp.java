package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class NullExp extends Expression {
	
	public VarType type = null; // FIX implement null exceptions, set type via the linker
	
	public NullExp(UnitSource source, int sourceStart, int sourceStop) {
		super(source, sourceStart, sourceStop);
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
