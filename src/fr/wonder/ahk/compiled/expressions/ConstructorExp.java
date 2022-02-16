package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.utils.Utils;

public class ConstructorExp extends Expression {
	
	public final VarType constructorType;
	
	/** Set by the linker */
	public ConstructorPrototype constructor;
	
	public ConstructorExp(SourceReference sourceRef, VarType constructorType, Expression[] arguments) {
		super(sourceRef, arguments);
		this.constructorType = constructorType;
	}
	
	@Override
	public VarStructType getType() {
		return (VarStructType) type;
	}
	
	@Override
	public String toString() {
		return constructorType + "(" + Utils.toString(expressions) + ")";
	}

}
