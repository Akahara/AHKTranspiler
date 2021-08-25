package fr.wonder.ahk.compiled.expressions;

import java.util.Arrays;

import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.utils.ArrayOperator;

/**
 * A call to a variable (of function type) 
 * <blockquote><pre>
 * func void a() {
 *   b = a;
 *   b();
 * }
 * </pre></blockquote>
 */
public class FunctionCallExp extends FunctionExpression {
	
	// set by the linker
	public VarFunctionType functionType;
	
	public FunctionCallExp(SourceReference sourceRef, Expression function, Expression[] arguments) {
		super(sourceRef, ArrayOperator.add(arguments, function));
	}
	
	public Expression getFunction() {
		return expressions[expressions.length-1];
	}

	@Override
	public int argumentCount() {
		return expressions.length-1;
	}
	
	@Override
	public Expression[] getArguments() {
		return Arrays.copyOfRange(expressions, 0, expressions.length-1);
	}
	
	@Override
	public String toString() {
		return getFunction() + "(" + Utils.toString(getArguments()) + ")";
	}

	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		return functionType.returnType;
	}
	
}
