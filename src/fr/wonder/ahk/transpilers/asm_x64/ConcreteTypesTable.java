package fr.wonder.ahk.transpilers.asm_x64;

import java.util.Map;

import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarNativeType;
import fr.wonder.ahk.compiled.expressions.types.VarStrType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;

public class ConcreteTypesTable {
	
	public static final int POINTER_SIZE = 8;
	
	private static final Map<VarNativeType, Integer> NATIVE_SIZES = Map.of(
			VarType.BOOL, 1,
			VarType.FLOAT, 8,
			VarType.INT, 8);
	
	public static int getPointerSize(VarType type) {
		if(type instanceof VarNativeType) {
			return NATIVE_SIZES.get(type);
		} else if(type instanceof VarStructType ||
				type instanceof VarArrayType ||
				type instanceof VarStrType) {
			return POINTER_SIZE;
		} else {
			throw new IllegalArgumentException("Type " + type + " does not support pointer references");
		}
	}
	
}
