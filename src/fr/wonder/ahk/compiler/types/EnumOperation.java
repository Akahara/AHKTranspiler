package fr.wonder.ahk.compiler.types;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarEnumType;
import fr.wonder.ahk.compiled.expressions.types.VarNativeType;
import fr.wonder.ahk.compiled.expressions.types.VarType;

public class EnumOperation extends Operation {

	private EnumOperation(VarEnumType l, VarEnumType r, Operator o, VarType resultType) {
		super(l, r, o, resultType);
	}

	public static EnumOperation getOperation(VarEnumType l, VarEnumType r, Operator o) {
		if(!l.getBackingType().equals(r.getBackingType()))
			return null;
		
		if(o == Operator.EQUALS)
			return new EnumOperation(l, r, o, VarNativeType.BOOL);
		if(o == Operator.NEQUALS)
			return new EnumOperation(l, r, o, VarNativeType.BOOL);
		
		// TODO add combineable enums
		// @enum::foo | @enum::bar
		
		return null;
	}

	
	
}
