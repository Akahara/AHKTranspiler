package fr.wonder.ahk.transpilers.common_x64.instructions;

public class RepeatedInstruction implements Instruction {
	
	private final Instruction instruction;
	
	public RepeatedInstruction(Instruction instruction) {
		this.instruction = instruction;
	}
	
	@Override
	public String toString() {
		return "  rep " + instruction;
	}
	
}
