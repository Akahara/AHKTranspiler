package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.ahk.utils.ErrorWrapper;
import fr.wonder.ahk.utils.Utils;

/**
 * Replaces {@link FunctionCallExp} when the {@link FunctionCallExp#getFunction() function argument}
 * is a {@link VarExp}. The replacement is done by the linker.
 */
public class FunctionExp extends FunctionExpression {
	
	public final FunctionSection function;
	
	public FunctionExp(Unit unit, FunctionCallExp funcCall, FunctionSection function) {
		super(unit, funcCall.sourceStart, funcCall.sourceStop, funcCall.getArguments());
		this.function = function;
	}
	
	public Expression[] getArguments() {
		return expressions;
	}
	
	@Override
	public String toString() {
		return function.name + "(" + Utils.toString(getArguments()) + ")";
	}

	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		return function.returnType;
	}
}
