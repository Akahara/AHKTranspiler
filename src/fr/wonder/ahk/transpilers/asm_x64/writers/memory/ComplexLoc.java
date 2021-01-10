package fr.wonder.ahk.transpilers.asm_x64.writers.memory;

public class ComplexLoc implements VarLocation {
	
	public final String base;
	public final int[] offsets;
	
	public ComplexLoc(String base, int[] offsets) {
		this.base = base;
		this.offsets = offsets;
	}
	
	public ComplexLoc(String base, int offset) {
		this(base, new int[] { offset });
	}
	
	public ComplexLoc(String base, int o1, int o2) {
		this(base, new int[] { o1, o2 });
	}
	
	public ComplexLoc(String base, int o1, int o2, int o3) {
		this(base, new int[] { o1, o2, o3 });
	}
	
	@Override
	public String getLoc() {
		throw new IllegalAccessError("Complex locations cannot be accessed dirrectly");
	}
	
}
