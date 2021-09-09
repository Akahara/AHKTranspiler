package fr.wonder.ahk.compiled.expressions.types;

public class VarGenericType extends VarType {
	
	private final String name;
	
	public VarGenericType(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getSignature() {
		return "G0";
	}

	@Override
	public VarType[] getSubTypes() {
		return new VarType[0];
	}

	@Override
	public boolean equals(Object o) {
		return o instanceof VarGenericType && ((VarGenericType) o).name.equals(name); // TODO fix generic type equality
	}

	public boolean isValidBinding(VarType varType) {
		return true;
	}
	
	@Override
	public boolean hasGenericTyping() {
		return true;
	}
	
}
