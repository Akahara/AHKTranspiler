package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class VarExp extends Expression {
	
	public final String variable;
	
	// set by the linker
	public VarAccess declaration;
	
	public VarExp(UnitSource source, int sourceStart, int sourceStop, String variable) {
		super(source, sourceStart, sourceStop);
		this.variable = variable;
	}
	
	@Override
	public String toString() {
		return variable;
	}

	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		return declaration.getType();
	}

}
