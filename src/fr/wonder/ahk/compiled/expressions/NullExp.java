package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;

public class NullExp extends Expression {
	
	public NullExp(SourceReference sourceRef) {
		super(sourceRef);
		this.type = VarType.NULL;
	}
	
	@Override
	public String toString() {
		return "null";
	}
	
	public void setNullType(VarType actualType) {
		this.type = actualType;
	}

}
