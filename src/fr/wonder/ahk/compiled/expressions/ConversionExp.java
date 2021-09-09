package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;

public class ConversionExp extends Expression {
	
	public final VarType castType;
	public final boolean isImplicit;
	
	public ConversionExp(SourceReference sourceRef, VarType castType, Expression value, boolean isImplicit) {
		super(sourceRef, value);
		this.castType = castType;
		this.isImplicit = isImplicit;
	}
	
	/** Used by the linker only, to cast function argument (for implicit conversions) for example */
	public ConversionExp(Expression value, VarType castType) {
		this(value.sourceRef, castType, value, true);
		this.type = castType;
	}
	
	public Expression getValue() {
		return expressions[0];
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
