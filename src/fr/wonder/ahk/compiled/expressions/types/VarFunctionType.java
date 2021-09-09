package fr.wonder.ahk.compiled.expressions.types;

import java.util.Objects;

import fr.wonder.ahk.compiled.units.sections.GenericContext;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.utils.ArrayOperator;

public class VarFunctionType extends VarType {
	
	public static final int MAX_LAMBDA_ARGUMENT_COUNT = 8;
	
	public final VarType returnType;
	public final VarType[] arguments;
	public final GenericContext genericContext;
	
	private String signature;
	
	public VarFunctionType(VarType returnType, VarType[] arguments, GenericContext genericContext) {
		this.returnType = returnType;
		this.arguments = arguments;
		this.genericContext = genericContext;
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
	public VarType[] getSubTypes() {
		return ArrayOperator.add(arguments, returnType);
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