package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.types.ConversionTable;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class ConversionExp extends Expression {
	
	public final VarType castType;
	public final boolean isImplicit;
	
	public ConversionExp(UnitSource source, int sourceStart, int sourceStop, VarType castType, Expression value, boolean isImplicit) {
		super(source, sourceStart, sourceStop, value);
		this.castType = castType;
		this.isImplicit = isImplicit;
	}
	
	/** Used by the linker only, to cast function argument (for implicit conversions) for example */
	public ConversionExp(Expression value, VarType castType) {
		this(value.getSource(), value.sourceStart, value.sourceStop, castType, value, true);
		this.type = castType;
	}
	
	public Expression getValue() {
		return expressions[0];
	}
	
	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		if(!ConversionTable.canConvertImplicitely(getValue().getType(), castType) &&
			(isImplicit || !ConversionTable.canConvertExplicitely(getValue().getType(), castType)))
			errors.add("Unable to convert explicitely from type " + getValue().getType() + " to " + castType);
		return castType;
	}
	
	@Override
	public String toString() {
		return castType+":("+getValue()+")";
	}

	/** Returns true if the type of the casted expression is not the destination type */
	public boolean isEffective() {
		return getValue().getType() != castType;
	}
	
}
