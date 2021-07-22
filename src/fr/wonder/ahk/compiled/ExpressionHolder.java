package fr.wonder.ahk.compiled;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.units.SourceElement;

public interface ExpressionHolder extends SourceElement {
	
	Expression[] getExpressions();
	
}
