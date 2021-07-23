package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.expressions.ConversionExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.units.SourceElement;

/**
 * <p>An expression holder is a source element that contains
 * one or more expressions that must be linked.
 * 
 * <p>In order for these to be linked they must be contained
 * by an array and only referenced through this array as the
 * linker may have to replace some or all of these expressions
 * by new ones (for implicit conversions for example, a {@code VarExp}
 * may be replaced by a {@link ConversionExp} with as value said
 * {@code VarExp}).
 */
public interface ExpressionHolder extends SourceElement {
	
	/** Returns the linkable expressions array, see {@link ExpressionHolder} */
	Expression[] getExpressions();
	
}
