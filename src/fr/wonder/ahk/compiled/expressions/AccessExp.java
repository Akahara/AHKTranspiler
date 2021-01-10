package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.ahk.utils.ErrorWrapper;

public class AccessExp extends Expression {
	
	public final String element;
	
	public AccessExp(Unit unit, int sourceStart, int sourceStop, Expression struct, String element) {
		super(unit, sourceStart, sourceStop, struct);
		this.element = element;
	}
	
	public Expression getStruct() {
		return expressions[0];
	}
	
	@Override
	public String toString() {
		return getStruct()+"."+element;
	}
	
	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		throw new IllegalAccessError(">>> Unimplemented");
	}

}
