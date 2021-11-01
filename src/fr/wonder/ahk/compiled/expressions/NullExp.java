package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarNullType;
import fr.wonder.ahk.compiled.units.SourceReference;

public class NullExp extends Expression {
	
	public NullExp(SourceReference sourceRef) {
		super(sourceRef);
		this.type = new VarNullType();
	}
	
	public VarNullType getType() {
		return (VarNullType) type;
	}
	
	@Override
	public String toString() {
		return "null";
	}

}
