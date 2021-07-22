package fr.wonder.ahk.transpilers.common_x64;

public enum MemSize {
	
	BYTE("byte",  'c', "db", "resb", 1),
	WORD("word",  'w', "dw", "resw", 2),
	DWORD("dword", 'l', "dd", "resd", 4),	// (double/long word)
	QWORD("qword", 'q', "dq", "resq", 8);
//			SFLOAT = new MemSize("sfloat", 's', "dd", 4),
//			DFLOAT = new MemSize("dfloat", 'd', "dq", 8);

	/** Alias for QWORD */
	public static final MemSize POINTER = QWORD;
	/** The size (in bytes) of a pointer in 64 bits mode */
	public static final int POINTER_SIZE = POINTER.bytes;
	
	public final String name;
	public final char prefix;
	public final String declaration;
	public final String reservation;
	public final int bytes;
	
	private MemSize(String name, char prefix, String declaration, String reservation, int bytes) {
		this.name = name;
		this.prefix = prefix;
		this.declaration = declaration;
		this.reservation = reservation;
		this.bytes = bytes;
	}
	
	private static MemSize[] values = values();
	
	public static MemSize getSize(int bytes) {
		for(MemSize s : values)
			if(s.bytes == bytes)
				return s;
		throw new IllegalArgumentException("Invalid memory size " + bytes);
	}

}
