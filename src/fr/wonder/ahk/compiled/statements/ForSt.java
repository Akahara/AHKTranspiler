package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.commons.annotations.NonNull;

public class ForSt extends LabeledStatement {
	
	public final VariableDeclaration declaration; // FIX the ForSt expressions are not in a single array (expression holder)
	public final AffectationSt affectation;
	
	public ForSt(UnitSource source, int sourceStart, int sourceStop, boolean singleLine,
			VariableDeclaration declaration, Expression condition, AffectationSt affectation) {
		
		super(source, sourceStart, sourceStop, singleLine, condition);
		this.declaration = declaration;
		this.affectation = affectation;
	}
	
	@NonNull
	public Expression getCondition() {
		return expressions[0];
	}
	
	@Override
	public String toString() {
		return "for(" + 
				(declaration == null ? "" : declaration.toString()) + ", " +
				getCondition() + ", " +
				(affectation == null ? "" : affectation) + ")" +
				(singleLine ? "" : "{");
	}

}
