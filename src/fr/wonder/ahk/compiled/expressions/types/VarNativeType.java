package fr.wonder.ahk.compiled.expressions.types;

public class VarNativeType extends VarType {
	
	private final String name;
	
	VarNativeType(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String getSignature() {
		return String.valueOf(name.charAt(0));
	}
	
	@Override
	public VarType[] getSubTypes() {
		return new VarType[0];
	}

	/**
	 * Native types are only created in {@link VarType} and only one instance
	 * of each exist at any point in time so the '==' operator can be used safely.
	 */
	@Override
	public boolean equals(Object o) {
		return this == o;
	}
	
}