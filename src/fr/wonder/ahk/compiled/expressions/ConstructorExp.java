package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.utils.Utils;

public class ConstructorExp extends Expression {
	
	/** Reference to the structure type, validated by the linker */
	public final VarType constructorType;
	
	/** Set by the linker */
	public ConstructorPrototype constructor;
	
	public ConstructorExp(SourceReference sourceRef, VarType type, Expression[] arguments) {
		super(sourceRef, arguments);
		this.constructorType = type;
	}
	
	@Override
	public String toString() {
		return constructorType + "(" + Utils.toString(expressions) + ")";
	}

}
