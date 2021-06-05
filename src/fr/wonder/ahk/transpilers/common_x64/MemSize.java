package fr.wonder.ahk.transpilers.common_x64;

import java.util.Map;

import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarNativeType;
import fr.wonder.ahk.compiled.expressions.types.VarStrType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;

public enum MemSize {
	
	BYTE("byte",  'c', "db", 1),
	WORD("word",  'w', "dw", 2),
	DWORD("dword", 'l', "dd", 4),	// (double/long word)
	QWORD("qword", 'q', "dq", 8);
//			SFLOAT = new MemSize("sfloat", 's', "dd", 4),
//			DFLOAT = new MemSize("dfloat", 'd', "dq", 8);

	private static final Map<VarNativeType, MemSize> NATIVE_SIZES = Map.of(
			VarType.BOOL, BYTE,
			VarType.FLOAT, QWORD,
			VarType.INT, QWORD);
	
	/** Alias for QWORD */
	public static final MemSize POINTER = QWORD;
	/** The size (in bytes) of a pointer in 64 bits mode */
	public static final int POINTER_SIZE = POINTER.bytes;
	
	public final String name;
	public final char prefix;
	public final String declaration;
	public final int bytes;
	
	private MemSize(String name, char prefix, String declaration, int bytes) {
		this.name = name;
		this.prefix = prefix;
		this.declaration = declaration;
		this.bytes = bytes;
	}
	
	private static MemSize[] values = values();
	
	public static MemSize getSize(int bytes) {
		for(MemSize s : values)
			if(s.bytes == bytes)
				return s;
		throw new IllegalArgumentException("Invalid memory size " + bytes);
	}

	/**
	 * Returns the size (in bytes) of a pointer to the given data type,
	 * if #type is a native type (bool, int...) its size is returned instead
	 * (BYTE for bool, QWORD for int....)
	 */
	public static MemSize getPointerSize(VarType type) {
		if(type instanceof VarNativeType) {
			return MemSize.NATIVE_SIZES.get(type);
		} else if(type instanceof VarStructType ||
				type instanceof VarArrayType ||
				type instanceof VarStrType) {
			return POINTER;
		} else {
			throw new IllegalArgumentException("Type " + type + " does not support pointer references");
		}
	}

}
