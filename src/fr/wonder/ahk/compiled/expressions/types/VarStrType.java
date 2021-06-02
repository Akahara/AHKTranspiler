package fr.wonder.ahk.compiled.expressions.types;

public class VarStrType extends VarType {

	@Override
	public String getName() {
		return "str";
	}

	@Override
	public String getSignature() {
		return "s";
	}

	@Override
	public boolean equals(Object o) {
		return o == VarType.STR;
	}

}