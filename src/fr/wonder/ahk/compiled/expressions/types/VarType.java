package fr.wonder.ahk.compiled.expressions.types;

import fr.wonder.ahk.compiled.expressions.NullExp;

public abstract class VarType {
	
	/**
	 * Used to avoid exceptions when an the type of an expression cannot be computed
	 * <br>Does <b>NOT<b> represent the type of the {@link NullExp null expression}
	 */
	public static final VarNativeType NULL = new VarNativeType("NULL");
	
	public static final VarNativeType VOID = new VarNativeType("void");
	public static final VarNativeType INT = new VarNativeType("int");
	public static final VarNativeType FLOAT = new VarNativeType("float");
	public static final VarNativeType BOOL = new VarNativeType("bool");
	public static final VarStructType STR = new VarStructType("str");

	public abstract String getName();
	public abstract String getSignature();
	
	@Override
	public String toString() {
		return getName();
	}

	public int getSize() {
		return 8;	// TODO have specific sizes for different data types
	}
	
	/**
	 * This method must be used rather than the '==' operator, multiple instances of the
	 * same logical variable type can exist at the same time (such as {@link VarArrayType}) 
	 */
	@Override
	public abstract boolean equals(Object o);
}
