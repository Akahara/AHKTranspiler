package fr.wonder.ahk.compiled.expressions.types;

public class VarSelfType extends VarType {
	
	public static final VarSelfType SELF = new VarSelfType();
	
	@Override
	public String getName() {
		return "Self";
	}

	@Override
	public String getSignature() {
		return "T";
	}

	@Override
	public VarType[] getSubTypes() {
		return NO_SUBTYPES;
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof VarSelfType;
	}
	
}
