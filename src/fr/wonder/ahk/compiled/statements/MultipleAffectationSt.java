package fr.wonder.ahk.compiled.statements;

import java.util.Arrays;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.utils.ArrayOperator;

public class MultipleAffectationSt extends Statement {
	
	private final int variableCount;

	public MultipleAffectationSt(SourceReference sourceRef, int sourceVariablesStop,
			Expression[] variables, Expression[] values) {
		super(sourceRef, ArrayOperator.add(variables, values));
		this.variableCount = variables.length;
	}
	
	/** a,b = f(); */
	public boolean isUnwrappedFunction() {
		return expressions.length == variableCount + 1; // getValues().length == 1
	}
	
	/**
	 * Returns an array of {@link VarExp}s, should be stored temporarily instead of
	 * called multiple times because the returned array is computed for every call
	 * and cannot be modified between calls.
	 */
	public Expression[] getVariables() {
		return Arrays.copyOfRange(expressions, 0, variableCount);
	}
	
	/**
	 * Should be stored temporarily instead of called multiple times because the
	 * returned array is computed for every call and cannot be modified between
	 * calls.
	 */
	public Expression[] getValues() {
		return Arrays.copyOfRange(expressions, variableCount, expressions.length);
	}

	@Override
	public String toString() {
		return Utils.toString(getVariables()) + " = " + Utils.toString(getValues());
	}

}
