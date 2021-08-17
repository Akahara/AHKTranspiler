package fr.wonder.ahk.compiled.expressions.types;

public class VarVoidType extends VarType {

	@Override
	public String getName() {
		return "void";
	}

	@Override
	public String getSignature() {
		return "v";
	}
	
	@Override
	public VarType[] getSubTypes() {
		return new VarType[0];
	}

	@Override
	public boolean equals(Object o) {
		return o == VarType.VOID;
	}

}
