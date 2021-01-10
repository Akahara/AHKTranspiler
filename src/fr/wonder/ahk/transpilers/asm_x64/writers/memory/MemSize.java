package fr.wonder.ahk.transpilers.asm_x64.writers.memory;

public enum MemSize {
	
	BYTE('c', 1),
	WORD('w', 2),
	DWORD('l', 4),	// (double/long word)
	QWORD('q', 8),
	
	SFLOAT('s', 4),
	DFLOAT('d', 8);
	
	public final char prefix;
	public final int bytes;
	
	MemSize(char prefix, int bytes) {
		this.prefix = prefix;
		this.bytes = bytes;
	}
	
	public String getCast() {
		return name().toLowerCase();
	}
	
	private static MemSize[] values = values();
	
	public static MemSize getSize(int bytes) {
		for(MemSize s : values)
			if(s.bytes == bytes)
				return s;
		throw new IllegalArgumentException("Invalid memory size " + bytes);
	}

}
