package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.units.SourceReference;

public class ForEachSt extends LabeledStatement {

	public final VariableDeclaration declaration;
	
	public ForEachSt(SourceReference sourceRef, boolean singleLine,
			VariableDeclaration declaration, Expression iterable) {
		super(sourceRef, singleLine, iterable);
		this.declaration = declaration;
	}
	
	public Expression getIterable() {
		return expressions[0];
	}
	
	@Override
	public String toString() {
		return "for(" + declaration + " : " + getIterable() + ")";
	}
}
