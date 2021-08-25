package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.commons.annotations.NonNull;

public class ForSt extends LabeledStatement {
	
	public final VariableDeclaration declaration;
	public final AffectationSt affectation;
	
	public ForSt(SourceReference sourceRef, boolean singleLine,
			VariableDeclaration declaration, Expression condition, AffectationSt affectation) {
		
		super(sourceRef, singleLine, condition);
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
