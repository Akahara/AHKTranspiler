package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.ExpressionHolder;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.units.SourceObject;

public abstract class Statement extends SourceObject implements ExpressionHolder {
	
	protected final Expression[] expressions;
	
	public Statement(UnitSource source, int sourceStart, int sourceStop, Expression... expressions) {
		super(source, sourceStart, sourceStop);
		this.expressions = expressions;
	}
	
	@Override
	public Expression[] getExpressions() {
		return expressions;
	}
	
}
