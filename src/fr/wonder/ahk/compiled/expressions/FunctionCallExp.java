package fr.wonder.ahk.compiled.expressions;

import java.util.Arrays;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.exceptions.ErrorWrapper;

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
	
	public FunctionCallExp(UnitSource source, int sourceStart, int sourceEnd, Expression function, Expression[] arguments) {
		super(source, sourceStart, sourceEnd, function, arguments);
	}
	
	public Expression getFunction() {
		return expressions[0];
	}
	
	public Expression[] getArguments() {
		return Arrays.copyOfRange(expressions, 1, expressions.length);
	}
	
	@Override
	public String toString() {
		return getFunction() + "(" + Utils.toString(getArguments()) + ")";
	}

	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		return functionType;
	}

	public VarType[] getArgumentsTypes() {
		Expression[] arguments = getArguments();
		VarType[] args = new VarType[arguments.length];
		for(int i = 0; i < args.length; i++)
			args[i] = arguments[i].getType();
		return args;
	}
	
}
