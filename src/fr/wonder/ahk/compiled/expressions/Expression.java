package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiler.linker.ExpressionHolder;
import fr.wonder.commons.annotations.NonNull;

public abstract class Expression implements ExpressionHolder, SourceElement {
	
	public final SourceReference sourceRef;
	public final Expression[] expressions;
	
	/** Set by the linker */
	public VarType type;
	
	public Expression(SourceReference sourceRef, Expression... expressions) {
		this.sourceRef = sourceRef;
		this.expressions = expressions;
	}
	
	public Expression(SourceReference sourceRef, Expression e, Expression[] expressions) {
		this.sourceRef = sourceRef;
		Expression[] exps = new Expression[1 + expressions.length];
		exps[0] = e;
		for(int i = 0; i < expressions.length; i++)
			exps[i+1] = expressions[i];
		this.expressions = exps;
	}
	
	public VarType getType() {
		return type;
	}
	
	@Override
	public SourceReference getSourceReference() {
		return sourceRef;
	}
	
	@NonNull
	@Override
	public Expression[] getExpressions() {
		return expressions;
	}
	
	public abstract String toString();
	
}
