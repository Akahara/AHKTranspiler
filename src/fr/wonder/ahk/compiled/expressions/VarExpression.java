package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class VarExpression extends Expression {
	
	public final String variable;
	
	/** Set by the linker*/
	public VariableDeclaration var;
	
	public VarExpression(UnitSource source, int sourceStart, int sourceStop, String variable) {
		super(source, sourceStart, sourceStop);
		this.variable = variable;
	}
	
	@Override
	public String toString() {
		return variable;
	}

	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		return var.getType();
	}
	
}
