package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.units.SourceObject;
import fr.wonder.ahk.compiler.linker.ExpressionHolder;

public class ConstructorDefaultValue extends SourceObject implements ExpressionHolder {

	public final String name;
	private final Expression[] valueArray;
	
	public ConstructorDefaultValue(UnitSource source, int sourceStart, int sourceStop, String name, Expression value) {
		super(source, sourceStart, sourceStop);
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
	public String toString() {
		return name + "=" + getValue();
	}

}
