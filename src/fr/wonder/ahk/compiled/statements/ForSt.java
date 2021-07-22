package fr.wonder.ahk.compiled.statements;

import java.util.Arrays;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;

public class ForSt extends LabeledStatement {
	
	public final VariableDeclaration declaration; // FIX the ForSt expressions are not in a single array (expression holder)
	public final Expression condition;
	public final AffectationSt affectation;
	
	public ForSt(UnitSource source, int sourceStart, int sourceStop, boolean singleLine,
			VariableDeclaration declaration, Expression condition, AffectationSt affectation) {
		
		super(source, sourceStart, sourceStop, singleLine, unwrapExpressions(condition, affectation));
		this.declaration = declaration;
		this.condition = condition;
		this.affectation = affectation;
	}
	
	private static Expression[] unwrapExpressions(Expression condition, AffectationSt affectation) {
		if(affectation == null)
			return new Expression[] { condition };
		Expression[] expressions = Arrays.copyOf(affectation.expressions, affectation.expressions.length+1);
		if(condition != null)
			expressions[affectation.expressions.length] = condition;
		return expressions;
	}
	
	@Override
	public String toString() {
		return "for(" + 
				(declaration == null ? "" : declaration.toString()) + ", " +
				condition + ", " +
				(affectation == null ? "" : affectation) + ")" +
				(singleLine ? "" : "{");
	}

}
