package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
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
	
	public FunctionExp(UnitSource source, FunctionCallExp funcCall, FunctionPrototype function) {
		super(source, funcCall.sourceStart, funcCall.sourceStop, funcCall.getArguments());
		this.function = function;
	}
	
	public Expression[] getArguments() {
		return expressions;
	}
	
	@Override
	public String toString() {
		return function.name + "(" + Utils.toString(getArguments()) + ")"; // TODO0 modify the function prototype to be able to display Unit.func(...) instead of func(...)
	}

	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		return function.functionType.returnType;
	}
}
