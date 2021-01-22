package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnimplementedException;

public class NullExp extends Expression {
	
	public VarType type = null; // FIX implement, set type via the linker
	
	public NullExp(UnitSource source, int sourceStart, int sourceStop) {
		super(source, sourceStart, sourceStop);
		throw new UnimplementedException("Nulls are not implemented");
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
