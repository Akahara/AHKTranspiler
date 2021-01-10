package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.ExpressionHolder;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.units.SourceObject;
import fr.wonder.ahk.compiler.Unit;

public abstract class Statement extends SourceObject implements ExpressionHolder {
	
	public final Expression[] expressions;
	
	public Statement(Unit unit, int sourceStart, int sourceStop, Expression... expressions) {
		super(unit, sourceStart, sourceStop);
		this.expressions = expressions;
	}
	
	@Override
	public Expression[] getExpressions() {
		return expressions;
	}
	
}
