package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.UnitSource;

public abstract class FunctionExpression extends Expression {

	/** {@link FunctionCallExp} constructor */
	public FunctionExpression(UnitSource source, int sourceStart, int sourceStop, Expression function, Expression[] arguments) {
		super(source, sourceStart, sourceStop, function, arguments);
	}
	
	/** {@link FunctionExp} constructor */
	public FunctionExpression(UnitSource source, int sourceStart, int sourceStop, Expression[] arguments) {
		super(source, sourceStart, sourceStop, arguments);
	}

}
