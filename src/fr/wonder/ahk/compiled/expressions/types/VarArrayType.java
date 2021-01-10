package fr.wonder.ahk.compiled.expressions.types;

public class VarArrayType extends VarType {
	
	public final VarType componentType;
	
	public VarArrayType(VarType componentType) {
		this.componentType = componentType;
	}
	
	@Override
	public String getName() {
		return componentType.getName()+"[]";
	}
	
	@Override
	public String getSignature() {
		return "["+componentType.getSignature();
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof VarArrayType && ((VarArrayType) o).componentType.equals(componentType);
	}
	
}