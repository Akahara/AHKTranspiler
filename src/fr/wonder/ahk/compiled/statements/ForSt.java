package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.commons.utils.ArrayOperator;
import fr.wonder.commons.utils.Assertions;

public class ForSt extends LabeledStatement {
	
	public final VarType declarationType;
	public final String declarationVar, affectationVar;
	public final boolean hasDeclaration, hasAffectation;
	
	public ForSt(UnitSource source, int sourceStart, int sourceStop, boolean singleLine,
			VarType declarationType, String declarationVar, Expression declaration,
			Expression condition,
			String affectationVar, Expression affectation) {
		
		super(source, sourceStart, sourceStop, singleLine, unwrapExpressions(declaration, condition, affectation));
		this.declarationType = declarationType;
		this.declarationVar = declarationVar;
		this.affectationVar = affectationVar;
		this.hasDeclaration = declarationVar != null;
		this.hasAffectation = affectationVar != null;
	}
	
	private static Expression[] unwrapExpressions(Expression declaration, Expression condition, Expression affectation) {
		return ArrayOperator.removeNull(new Expression[] { condition, declaration, affectation });
	}
	
	public Expression getCondition() {
		return expressions[0];
	}
	
	public Expression getDeclarationValue() {
		Assertions.assertTrue(hasDeclaration, "This statement does not have a declaration");
		return expressions[1];
	}
	
	public Expression getAffectationValue() {
		
	}
	
	@Override
	public String toString() {
		return "for(" + 
				(hasDeclaration ? "" : declarationType + " " + declarationVar + " = " + getDeclarationValue()) + ", " +
				condition + ", " +
				(affectation == null ? "" : affectation) + ")" +
				(singleLine ? "" : "{");
	}

}
