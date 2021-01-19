package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;

public class ForSt extends LabeledStatement {
	
	public final VariableDeclaration declaration;
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
		int affectc = affectation == null ? 0 : affectation.expressions.length;
		int condc = condition == null ? 0 : 1;
		Expression[] expressions = new Expression[affectc + condc];
		for(int i = 0; i < affectc; i++)
			expressions[i] = affectation.expressions[i];
		if(condition != null)
			expressions[affectc] = condition;
		return expressions;
	}
	
	@Override
	public String toString() {
		return "for(" + 
				(declaration == null ? "" : declaration.toString()) + ", " +
				(condition == null ? "" : condition) + ", " +
				(affectation == null ? "" : affectation) + ")" +
				(singleLine ? "" : "{");
	}

}
