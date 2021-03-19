package fr.wonder.ahk.compiled.statements;

import java.util.Arrays;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.utils.Utils;

public class MultipleAffectationSt extends Statement {

	private final String[] variables;
	
	public MultipleAffectationSt(UnitSource source, int sourceStart, int sourceStop, int sourceVariablesStop,
			String[] variables, Expression[] values) {
		super(source, sourceStart, sourceStop, concatVarVal(
				source, sourceStart, sourceVariablesStop, variables, values));
		this.variables = variables;
	}
	
	private static Expression[] concatVarVal(UnitSource source, int sourceStart, int sourceStop,
			String[] variables, Expression[] values) {
		Expression[] expressions = Arrays.copyOfRange(values, 0, values.length + variables.length);
		for(int i = 0; i < variables.length; i++)
			expressions[i+values.length] = new VarExp(source, sourceStart, sourceStop, variables[i]);
		return expressions;
	}
	
	/** a,b = f(); */
	public boolean isUnwrappedFunction() {
		return getValues().length == 1;
	}
	
	public String[] getVariablesNames() {
		return variables;
	}
	
	/**
	 * Returns an array of {@link VarExp}s, should be stored temporarily instead of
	 * called multiple times because the returned array is computed for every call
	 * but cannot be modified between calls.
	 */
	public Expression[] getVariables() {
		return Arrays.copyOfRange(expressions, expressions.length - variables.length, expressions.length);
	}
	
	/**
	 * Should be stored temporarily instead of called multiple times because the
	 * returned array is computed for every call but cannot be modified between
	 * calls.
	 */
	public Expression[] getValues() {
		return Arrays.copyOfRange(expressions, 0, expressions.length - variables.length);
	}

	@Override
	public String toString() {
		return Utils.toString(getVariablesNames()) + " = " + Utils.toString(getValues());
	}

}
