package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class DirectAccessExp extends Expression {
	
	public final String element;
	
	public DirectAccessExp(UnitSource source, int sourceStart, int sourceStop, Expression struct, String element) {
		super(source, sourceStart, sourceStop, struct);
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
