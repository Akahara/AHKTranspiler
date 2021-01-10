package fr.wonder.ahk.transpilers.asm_x64.writers.memory;

public interface VarLocation {
	
	public String REG_RAX = "rax";
	public String REG_RBX = "rbx";
	
	public String REG_RSP = "rsp";
	public String REG_RBP = "rbp";
	
	String getLoc();
	
}
