package fr.wonder.ahk.compiled.expressions.types;

public abstract class VarType {
	
	public static final VarVoidType VOID = new VarVoidType();
	public static final VarNativeType INT = new VarNativeType("int");
	public static final VarNativeType FLOAT = new VarNativeType("float");
	public static final VarNativeType BOOL = new VarNativeType("bool");
	public static final VarStrType STR = new VarStrType();
	
	protected static final VarType[] NO_SUBTYPES = new VarType[0];
	
	/** Returns the user-friendly name of this type */
	public abstract String getName();
	/** Returns a non user-friendly descriptor (signature) of this type */
	public abstract String getSignature();
	/**
	 * Returns the set of types used to make this type.
	 * On function types it returns arguments and result types for example.
	 */
	public abstract VarType[] getSubTypes();
	
	@Override
	public String toString() {
		return getName();
	}
	
	/**
	 * Returns true if this type's hierarchy contains a generic type.
	 * <p>Only {@link VarGenericType} overrides this method
	 */
	public boolean hasGenericTyping() {
		for(VarType t : getSubTypes())
			if(t.hasGenericTyping())
				return true;
		return false;
	}

	/**
	 * This method must be used rather than the '==' operator, multiple instances of the
	 * same logical variable type can exist at the same time (such as {@link VarArrayType}) 
	 */
	@Override
	public abstract boolean equals(Object o);
	
}
