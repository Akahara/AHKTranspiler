package fr.wonder.ahk.compiled.expressions.types;

import java.util.Objects;

import fr.wonder.ahk.utils.Utils;

public class VarFunctionType extends VarType {
	
	public final VarType returnType;
	public final VarType[] arguments;
	
	private String signature;
	
	public VarFunctionType(VarType returnType, VarType[] arguments) {
		this.returnType = returnType;
		this.arguments = arguments;
	}

	@Override
	public String getName() {
		return "(func " + returnType + "(" + Utils.toString(arguments) + "))";
	}
	
	@Override
	public String getSignature() {
		if(signature != null)
			return signature;
		signature = returnType.getSignature();
		for(VarType arg : arguments)
			signature += arg.getSignature();
		return signature;
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof VarFunctionType && getSignature().equals(((VarFunctionType) o).getSignature());
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(returnType, arguments, getSignature());
	}
	
}