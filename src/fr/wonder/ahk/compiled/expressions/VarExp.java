package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class VarExp extends Expression {
	
	public final String variable;
	
	// set by the linker
	public VarType varType;
	public ValueDeclaration declaration;
	
	public VarExp(Unit unit, int sourceStart, int sourceStop, String variable) {
		super(unit, sourceStart, sourceStop);
		this.variable = variable;
	}
	
	@Override
	public String toString() {
		return variable;
	}

	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		return varType;
	}

}
