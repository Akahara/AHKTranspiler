package fr.wonder.ahk.transpilers.common_x64.instructions;

public class SpecialInstruction implements Instruction {
	
	public final String line;
	
	public SpecialInstruction(String line) {
		this.line = line;
	}
	
	@Override
	public String toString() {
		return line;
	}
	
}
