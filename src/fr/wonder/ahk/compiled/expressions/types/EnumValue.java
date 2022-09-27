package fr.wonder.ahk.compiled.expressions.types;

public class EnumValue {
	
	public final VarEnumType enumType;
	public final String valueName;
	
	public EnumValue(VarEnumType enumType, String valueName) {
		this.enumType = enumType;
		this.valueName = valueName;
	}
	
	@Override
	public String toString() {
		return enumType + "::" + valueName;
	}
	
}
