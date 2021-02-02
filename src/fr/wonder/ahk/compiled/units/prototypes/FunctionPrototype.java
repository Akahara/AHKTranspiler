package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;

public class FunctionPrototype implements VarAccess {

	public final String declaringUnit;
	public final String name;
	public final String signature;
	
	public final VarFunctionType functionType;
	
	public FunctionPrototype(String declaringUnit, String name, String signature, VarFunctionType functionType) {
		this.declaringUnit = declaringUnit;
		this.name = name;
		this.signature = signature;
		this.functionType = functionType;
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
