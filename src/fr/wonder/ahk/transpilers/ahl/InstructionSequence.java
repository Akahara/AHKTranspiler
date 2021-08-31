package fr.wonder.ahk.transpilers.ahl;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.transpilers.ahl.instructions.LInstruction;
import fr.wonder.ahk.transpilers.ahl.values.VPointer;

public class InstructionSequence {
	
	public final List<LInstruction> instructions = new ArrayList<>();
	
	private int nextVPointerId = 1;
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(LInstruction i : instructions) {
			sb.append(i.toString());
			sb.append('\n');
		}
		return sb.toString();
	}

	public VPointer vPointer() {
		return new VPointer(nextVPointerId++);
	}
	
}
