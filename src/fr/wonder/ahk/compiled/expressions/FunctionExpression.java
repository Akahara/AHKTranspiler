package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.commons.utils.ArrayOperator;

/**
 * This class is the base class for both {@link FunctionExp} (declared function
 * call) and {@link FunctionCallExp} (arbitrary function call). For the
 * Expression Linker to work these classes <b>must</b> define
 * {@link #argumentCount()} and the {@link #getExpressions() expressions} fields
 * of instances must start with the call arguments, the array may contain other
 * values (such as the actual function for {@code FunctionCallExp}).
 * 
 * <p>Note that {@link #getArguments()} does <b>not</b> return a modifiable
 * array of expressions, it can be read from but not written to.
 */
public abstract class FunctionExpression extends Expression {

	/** {@link FunctionCallExp} constructor */
	public FunctionExpression(SourceReference sourceRef, Expression function, Expression[] arguments) {
		super(sourceRef, function, arguments);
	}
	
	/** {@link FunctionExp} constructor */
	public FunctionExpression(SourceReference sourceRef, Expression[] arguments) {
		super(sourceRef, arguments);
	}
	
	public abstract int argumentCount();
	public abstract Expression[] getArguments();
	
	public VarType[] getArgumentsTypes() {
		return ArrayOperator.map(getArguments(), VarType[]::new, Expression::getType);
	}
	
}
