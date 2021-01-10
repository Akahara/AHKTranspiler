package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiler.Unit;

public abstract class FunctionExpression extends Expression {

	/** {@link FunctionCallExp} constructor */
	public FunctionExpression(Unit unit, int sourceStart, int sourceStop, Expression function, Expression[] arguments) {
		super(unit, sourceStart, sourceStop, function, arguments);
	}
	
	/** {@link FunctionExp} constructor */
	public FunctionExpression(Unit unit, int sourceStart, int sourceStop, Expression[] arguments) {
		super(unit, sourceStart, sourceStop, arguments);
	}

}
