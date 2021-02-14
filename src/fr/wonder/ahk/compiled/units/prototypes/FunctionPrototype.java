package fr.wonder.ahk.compiled.units.prototypes;

import java.util.Objects;

import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;

public class FunctionPrototype implements VarAccess {

	/** Full base of the unit declaring this function */
	public final String declaringUnit;
	/** The name of this function */
	public final String name;
	/** Contains the name information in some way */
	public final String signature;
	/** The type of this function, contains its arguments and return type */
	public final VarFunctionType functionType;
	
	public FunctionPrototype(String declaringUnit, String name, String signature, VarFunctionType functionType) {
		this.declaringUnit = Objects.requireNonNull(declaringUnit);
		this.name = Objects.requireNonNull(name);
		this.signature = Objects.requireNonNull(signature);
		this.functionType = Objects.requireNonNull(functionType);
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof FunctionPrototype))
			return false;
		FunctionPrototype p = (FunctionPrototype) o;
		if(!signature.equals(p.signature))
			return false;
		if(!functionType.returnType.equals(p.functionType.returnType))
			return false;
		if(functionType.arguments.length != p.functionType.arguments.length)
			return false;
		for(int i = 0; i < functionType.arguments.length; i++) {
			if(!functionType.arguments[i].equals(p.functionType.arguments[i]))
				return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return functionType.toString() + ":" + declaringUnit + "." + name;
	}

	@Override
	public String getUnitFullBase() {
		return declaringUnit;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getSignature() {
		return signature;
	}

	@Override
	public VarFunctionType getType() {
		return functionType;
	}

}
