package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.exceptions.ErrorWrapper;

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
		super(funcCall.getSource(), funcCall.sourceStart,
				funcCall.sourceStop, funcCall.getArguments());
		this.function = function;
	}
	
	/**
	 * Can only be used for linked operation expressions referring to overloaded
	 * operators.
	 */
	public FunctionExp(OperationExp operation) {
		super(operation.getSource(), operation.sourceStart,
				operation.sourceStop, operation.getOperands());
		this.function = ((OverloadedOperatorPrototype) operation.getOperation()).function;
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

	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		return function.functionType.returnType;
	}

}
