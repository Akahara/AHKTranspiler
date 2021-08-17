package fr.wonder.ahk.compiled.expressions.types;

public class VarStrType extends VarNativeType {

	VarStrType() {
		super("string");
	}

	@Override
	public String getName() {
		return "str";
	}

	@Override
	public String getSignature() {
		return "s";
	}

}