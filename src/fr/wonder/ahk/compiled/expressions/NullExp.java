package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class NullExp extends Expression {
	
	public NullExp(SourceReference sourceRef) {
		super(sourceRef);
	}
	
	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		return VarType.NULL;
	}

	@Override
	public String toString() {
		return "null";
	}
	
	public void setNullType(VarType actualType) {
		this.type = actualType;
	}

}
