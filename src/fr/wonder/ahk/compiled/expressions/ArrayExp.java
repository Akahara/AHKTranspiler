package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.utils.Utils;

public class ArrayExp extends Expression {
	
	public ArrayExp(SourceReference sourceRef, Expression[] values) {
		super(sourceRef, values);
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
	
	/** Override to cast to an Array type */
	@Override
	public VarArrayType getType() {
		return (VarArrayType) type;
	}
}
