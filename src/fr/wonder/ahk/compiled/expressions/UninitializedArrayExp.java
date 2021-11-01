package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.units.SourceReference;

public class UninitializedArrayExp extends Expression {
	
	private Expression defaultComponentValue;
	
	public UninitializedArrayExp(SourceReference sourceRef, Expression size) {
		super(sourceRef, size);
	}
	
	public Expression getSize() {
		return expressions[0];
	}
	
	public void setDefaultComponentValue(Expression defaultComponentValue) {
		this.defaultComponentValue = defaultComponentValue;
	}
	
	public Expression getDefaultComponentValue() {
		return defaultComponentValue;
	}
	
	@Override
	public VarArrayType getType() {
		return (VarArrayType) super.getType();
	}
	
	@Override
	public String toString() {
		return "[:" + getSize() + "]";
	}
	
}
