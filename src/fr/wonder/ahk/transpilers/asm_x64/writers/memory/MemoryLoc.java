package fr.wonder.ahk.transpilers.asm_x64.writers.memory;

public class MemoryLoc implements VarLocation {
	
	String base;
	String index;
	int scale;
	int offset;
	
	public MemoryLoc(String base, String index, int scale, int offset) {
		this.base = base;
		this.index = index;
		this.scale = scale;
		this.offset = offset;
	}
	
	public MemoryLoc(String base, String index, int offset) {
		this(base, index, 1, offset);
	}
	
	public MemoryLoc(String base, String index) {
		this(base, index, 1, 0);
	}
	
	public MemoryLoc(String base, int offset) {
		this(base, null, 1, offset);
	}
	
	public MemoryLoc(String base) {
		this(base, null, 1, 0);
	}

	@Override
	public String getLoc() {
		String loc = "[" + base;
		if(index != null) {
			loc += "+" + index;
			if(scale != 1)
				loc += "*" + scale;
		}
		if(offset != 0)
			loc += "+" + offset;
		loc += "]";
		return loc;
	}
	
}
