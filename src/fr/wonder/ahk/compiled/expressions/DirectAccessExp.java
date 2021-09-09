package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;

public class DirectAccessExp extends Expression {
	
	public final String memberName;
	/** Set by the linker */
	public VariablePrototype member;
	
	public DirectAccessExp(SourceReference sourceRef, Expression structInstance, String memberName) {
		super(sourceRef, structInstance);
		this.memberName = memberName;
	}
	
	public Expression getStruct() {
		return expressions[0];
	}
	
	/** Cannot be used safely before the linker types check */
	public VarStructType getStructType() {
		return (VarStructType) getStruct().getType();
	}
	
	@Override
	public String toString() {
		return getStruct()+"."+memberName;
	}

}
