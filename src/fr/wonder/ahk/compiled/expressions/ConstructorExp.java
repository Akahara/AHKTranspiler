package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class ConstructorExp extends Expression {
	
	private final VarStructType type;
	
	/** Set by the linker */
	public ConstructorPrototype constructor;
	
	public ConstructorExp(UnitSource source, int sourceStart, int sourceStop, VarStructType type, Expression[] arguments) {
		super(source, sourceStart, sourceStop, arguments);
		this.type = type;
	}

	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		return type;
	}
	
	@Override
	public VarStructType getType() {
		return type;
	}

	@Override
	public String toString() {
		return type.name + "(" + Utils.toString(expressions) + ")";
	}

}
