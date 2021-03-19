package fr.wonder.ahk.compiled.expressions.types;

public abstract class VarType {
	
	public static final VarNativeType VOID = new VarNativeType("void");
	public static final VarNativeType INT = new VarNativeType("int");
	public static final VarNativeType FLOAT = new VarNativeType("float");
	public static final VarNativeType BOOL = new VarNativeType("bool");
	
	public static boolean isNumber(VarType type) {
		return type == INT || type == FLOAT;
	}
	
	public static final VarStructType STR = new VarStructType("str") { // FIX rework the string type
		@Override
		public String getSignature() {
			return "s";
		}
	};
	
	/** Returns the user-friendly name of this type */
	public abstract String getName();
	/** Returns a non user-friendly descriptor (signature) of this type */
	public abstract String getSignature();
	
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
