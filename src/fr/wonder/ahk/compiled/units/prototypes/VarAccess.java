package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.types.VarType;

public interface VarAccess {
	
	public static final String INNER_UNIT = "local";
	
	public String getDeclaringUnit();
	public String getName();
	public String getSignature();
	public VarType getType();
	public default String getUnitName() {
		String fullBase = getDeclaringUnit();
		return fullBase.substring(fullBase.lastIndexOf('.')+1);
	}
	
}
