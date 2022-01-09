package fr.wonder.ahk.compiled.expressions.types;

public class VarNullType extends VarType {

	private VarType actualType;
	
	public VarNullType() {
		
	}
	
	@Override
	public String getName() {
		return "null";
	}
	
	public VarType getActualType() {
		return actualType;
	}
	
	public void setActualType(VarType actualType) {
		this.actualType = actualType;
	}

	@Override
	public String getSignature() {
		throw new IllegalAccessError("The null type does not have a signature");
	}
	
	/**
	 * This method returns no sub types as the actual type is
	 * computed using types that the enclosing unit can access.
	 */
	@Override
	public VarType[] getSubTypes() {
		return NO_SUBTYPES;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof VarNullType && ((VarNullType) o).actualType.equals(actualType);
	}

	/**
	 * Returns whether {@code null} can be used as a value for a variable of the
	 * given type . Acceptable types are {@link VarStructType}, {@link VarArrayType},
	 * {@link VarFunctionType} and {@link VarGenericType} in some cases
	 */
	public static boolean isAcceptableNullType(VarType type) {
		return type instanceof VarStructType ||
				type instanceof VarArrayType ||
				type instanceof VarFunctionType ||
				type instanceof VarGenericType ||
				type == VarType.STR;
	}
	
	
}
