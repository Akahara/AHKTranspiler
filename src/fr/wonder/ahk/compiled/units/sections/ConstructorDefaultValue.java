package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiler.linker.ExpressionHolder;

public class ConstructorDefaultValue implements ExpressionHolder {

	public final SourceReference sourceRef;
	public final String name;
	private final Expression[] valueArray;
	
	public ConstructorDefaultValue(SourceReference sourceRef, String name, Expression value) {
		this.sourceRef = sourceRef;
		this.name = name;
		this.valueArray = new Expression[] { value };
	}
	
	public Expression getValue() {
		return valueArray[0];
	}

	@Override
	public Expression[] getExpressions() {
		return valueArray;
	}
	
	@Override
	public SourceReference getSourceReference() {
		return sourceRef;
	}
	
	@Override
	public String toString() {
		return name + "=" + getValue();
	}

}
