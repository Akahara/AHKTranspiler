package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiler.linker.ExpressionHolder;

public abstract class Statement implements ExpressionHolder, SourceElement {
	
	public final SourceReference sourceRef;
	protected final Expression[] expressions;
	
	public Statement(SourceReference sourceRef, Expression... expressions) {
		this.sourceRef = sourceRef;
		this.expressions = expressions;
	}
	
	@Override
	public SourceReference getSourceReference() {
		return sourceRef;
	}
	
	@Override
	public Expression[] getExpressions() {
		return expressions;
	}
	
	public abstract String toString();
	
}
