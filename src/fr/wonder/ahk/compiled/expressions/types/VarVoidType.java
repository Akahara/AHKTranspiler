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

	/**
	 * A single instance of the void type may exist at any point so '==' can be used
	 * safely here
	 */
	@Override
	public boolean equals(Object o) {
		return o == VarType.VOID;
	}

}
