package fr.wonder.ahk.transpilers.ahl.values;

import fr.wonder.ahk.compiled.expressions.types.VarNativeType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.commons.utils.Assertions;

public class LiteralValue implements Value {
	
	public final VarType type;
	public final Object value;
	
	public LiteralValue(VarType type, Object value) {
		this.type = type;
		this.value = value;
		Assertions.assertTrue(type instanceof VarNativeType, "Literals must have native types");
	}
	
}
