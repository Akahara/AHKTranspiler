package fr.wonder.ahk.transpilers.asm_x64.writers.memory;

public class DirectLoc implements VarLocation {
	
	public static final DirectLoc LOC_RAX = new DirectLoc(REG_RAX);
	public static final DirectLoc LOC_RBX = new DirectLoc(REG_RBX);
	
	String loc;
	
	DirectLoc(String loc) {
		this.loc = loc;
	}
	
	@Override
	public String getLoc() {
		return loc;
	}
	
}
