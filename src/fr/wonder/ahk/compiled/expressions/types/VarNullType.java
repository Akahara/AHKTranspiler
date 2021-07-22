package fr.wonder.ahk.compiled.expressions.types;

public class VarNullType extends VarType {

	public static VarNullType INSTANCE = new VarNullType();
	
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
	public boolean equals(Object o) {
		return o == this;
	}

	public static boolean isAcceptableNullType(VarType type) {
		return type instanceof VarStructType ||
				type instanceof VarArrayType ||
				type instanceof VarFunctionType;
	}
	
	
}
