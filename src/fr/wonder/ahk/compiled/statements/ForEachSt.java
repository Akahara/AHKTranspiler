package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;

public class ForEachSt extends LabeledStatement {

	public final VariableDeclaration declaration;
	
	public ForEachSt(UnitSource source, int sourceStart, int sourceStop, boolean singleLine,
			VariableDeclaration declaration, Expression iterable) {
		super(source, sourceStart, sourceStop, singleLine, iterable);
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
