package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.utils.Utils;

/**
 * Replaces {@link FunctionCallExp} when the {@link FunctionCallExp#getFunction() function argument}
 * is a {@link VarExp}. The replacement is done by the linker.
 * 
 * <blockquote><pre>
 * func void a() {
 *   a(); // FunctionExp
 * }
 * </pre></blockquote>
 */
public class FunctionExp extends FunctionExpression {
	
	public final FunctionPrototype function;

	public FunctionExp(FunctionCallExp funcCall, FunctionPrototype function) {
		super(funcCall.sourceRef, funcCall.getArguments());
		this.function = function;
	}
	
	@Override
	public int argumentCount() {
		return getArguments().length;
	}
	
	@Override
	public Expression[] getArguments() {
		return expressions;
	}
	
	@Override
	public String toString() {
		return function.signature.declaringUnit + "." + function.getName() +
				"(" + Utils.toString(getArguments()) + ")";
	}

}
