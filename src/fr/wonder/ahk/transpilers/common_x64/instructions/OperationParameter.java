package fr.wonder.ahk.transpilers.common_x64.instructions;

import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.addresses.ImmediateValue;
import fr.wonder.ahk.transpilers.common_x64.addresses.LabelAddress;
import fr.wonder.commons.utils.ArrayOperator;

public interface OperationParameter {
	
	@Override
	String toString();
	
	

	public static Object asExtendedOperationParameter(Object param) {
		if(param instanceof MemSize)
			return param;
		return asOperationParameter(param);
	}

	public static Object[] asExtendedOperationParameters(Object... params) {
		return ArrayOperator.map(
				params,
				OperationParameter::asExtendedOperationParameter);
	}

	public static OperationParameter asOperationParameter(Object param) {
		if(param instanceof OperationParameter)
			return (OperationParameter) param;
		if(param instanceof String)
			return new LabelAddress((String) param);
		else if(param instanceof Integer)
			return new ImmediateValue((Integer) param);
		
		throw new IllegalArgumentException("Unknown operand type: " + param.getClass() + " " + param);
	}

	public static OperationParameter[] asOperationParameters(Object... params) {
		return ArrayOperator.map(
						params, 
						OperationParameter[]::new,
						OperationParameter::asOperationParameter);
	}
	
}
