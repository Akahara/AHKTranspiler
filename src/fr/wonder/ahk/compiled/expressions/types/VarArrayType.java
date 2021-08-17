package fr.wonder.ahk.compiled.expressions.types;

public class VarArrayType extends VarType {
	
	public final VarType componentType;
	
	public static final VarArrayType EMPTY_ARRAY = new VarArrayType(VarType.VOID);
	
	public VarArrayType(VarType componentType) {
		this.componentType = componentType;
	}
	
	@Override
	public String getName() {
		return componentType.getName()+"[]";
	}
	
	@Override
	public String getSignature() {
		return "A"+componentType.getSignature();
	}
	
	@Override
	public VarType[] getSubTypes() {
		return new VarType[] { componentType };
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof VarArrayType && ((VarArrayType) o).componentType.equals(componentType);
	}
	
}