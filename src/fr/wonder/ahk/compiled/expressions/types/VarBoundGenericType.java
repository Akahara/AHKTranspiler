package fr.wonder.ahk.compiled.expressions.types;

public class VarBoundGenericType extends VarGenericType {

	public VarBoundGenericType(VarGenericType original) {
		super(original.typeParameter);
	}
	
	@Override
	public boolean hasGenericTyping() {
		return false;
	}

}
