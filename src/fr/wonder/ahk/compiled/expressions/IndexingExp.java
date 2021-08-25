package fr.wonder.ahk.compiled.expressions;

import java.util.Arrays;

import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class IndexingExp extends Expression {
	
	public IndexingExp(SourceReference sourceRef, Expression array, Expression[] indices) {
		super(sourceRef, array, indices);
	}
	
	public Expression getArray() {
		return expressions[0];
	}
	
	public Expression[] getIndices() {
		return Arrays.copyOfRange(expressions, 1, expressions.length);
	}
	
	@Override
	public String toString() {
		return getArray() + "[" + Utils.toString(getIndices()) + "]";
	}

	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		VarType type = getArray().getType();
		int indicesCount = expressions.length-1;
		for(int i = 0; i < indicesCount; i++) {
			if(type instanceof VarArrayType) {
				type = ((VarArrayType) type).componentType;
			} else {
				errors.add("Type " + type + " cannot be indexed " + getErr());
				return Invalids.TYPE;
			}
		}
		return type;
	}
	
	public IndexingExp subIndexingExpression() {
		if(!(this.type instanceof VarArrayType))
			throw new IllegalStateException("Cannot sub-index a non-array typed indexing expression");
		Expression[] subIndices = Arrays.copyOfRange(expressions, 1, expressions.length-2);
		IndexingExp subExp = new IndexingExp(sourceRef, getArray(), subIndices);
		subExp.type = ((VarArrayType) this.type).componentType;
		return subExp;
	}
	
}
