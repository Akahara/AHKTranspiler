package fr.wonder.ahk.compiled.expressions.types;

import fr.wonder.ahk.compiled.expressions.NullExp;

public abstract class VarType {
	
	public static final VarVoidType VOID = new VarVoidType();
	public static final VarNativeType INT = new VarNativeType("int");
	public static final VarNativeType FLOAT = new VarNativeType("float");
	public static final VarNativeType BOOL = new VarNativeType("bool");
	public static final VarStrType STR = new VarStrType();
	
	/**
	 * The null type is only used for {@link NullExp Null expressions}, their
	 * "actual type" is computed by the linker whenever required, see
	 * Linker#checkAffectationType (package method so no link available)
	 */
	public static final VarNullType NULL = VarNullType.INSTANCE;
	
	/** Returns the user-friendly name of this type */
	public abstract String getName();
	/** Returns a non user-friendly descriptor (signature) of this type */
	public abstract String getSignature();
	
	public abstract VarType[] getSubTypes();
	
	@Override
	public String toString() {
		return getName();
	}

	/**
	 * This method must be used rather than the '==' operator, multiple instances of the
	 * same logical variable type can exist at the same time (such as {@link VarArrayType}) 
	 */
	@Override
	public abstract boolean equals(Object o);
	
}
