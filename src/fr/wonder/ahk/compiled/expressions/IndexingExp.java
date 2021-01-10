package fr.wonder.ahk.compiled.expressions;

import java.util.Arrays;

import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.ahk.utils.ErrorWrapper;

public class IndexingExp extends Expression {
	
	public IndexingExp(Unit unit, int sourceStart, int sourceStop, Expression array, Expression[] indices) {
		super(unit, sourceStart, sourceStop, array, indices);
	}
	
	public Expression getArray() {
		return expressions[0];
	}
	
	public Expression[] getIndices() {
		return Arrays.copyOfRange(expressions, 1, expressions.length);
	}
	
	@Override
	public String toString() {
		return getArray() + "[" + getIndices() + "]";
	}

	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		VarType type = getArray().getType();
		if(type instanceof VarArrayType) {
			return ((VarArrayType) type).componentType;
		} else {
			errors.add("Type " + type + " cannot be indexed " + getErr());
			return VarType.NULL;
		}
	}
	
}
