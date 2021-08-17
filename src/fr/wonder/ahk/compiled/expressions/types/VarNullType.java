package fr.wonder.ahk.compiled.expressions.types;

public class VarNullType extends VarType {

	public static final VarNullType INSTANCE = new VarNullType();
	
	private VarNullType() {}
	
	@Override
	public String getName() {
		return "null";
	}

	@Override
	public String getSignature() {
		throw new IllegalAccessError("The null type does not have a signature");
	}
	
	@Override
	public VarType[] getSubTypes() {
		return new VarType[0];
	}

	/**
	 * A single instance of the null type may exist at any point so '==' can be used
	 * safely here
	 */
	@Override
	public boolean equals(Object o) {
		return o == this;
	}

	/**
	 * Returns whether {@code null} can be used as a value for a variable of the
	 * given type . Acceptable types are {@link VarStructType}, {@link VarArrayType}
	 * and {@link VarFunctionType}.
	 */
	public static boolean isAcceptableNullType(VarType type) {
		return type instanceof VarStructType ||
				type instanceof VarArrayType ||
				type instanceof VarFunctionType;
	}
	
	
}
