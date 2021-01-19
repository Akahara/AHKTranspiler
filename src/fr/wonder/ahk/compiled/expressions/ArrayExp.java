package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class ArrayExp extends Expression {
	
	/** Set by the linker */
	public VarType type;

	public ArrayExp(UnitSource source, int sourceStart, int sourceStop, Expression[] values) {
		super(source, sourceStart, sourceStop, values);
	}
	
	public Expression[] getValues() {
		return expressions;
	}
	
	public int getLength() {
		return expressions.length;
	}
	
	@Override
	public String toString() {
		return "[" + Utils.toString(getValues()) + "]";
	}

	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		return type;
	}
}
