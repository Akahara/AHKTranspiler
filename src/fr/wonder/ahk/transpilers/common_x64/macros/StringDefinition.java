package fr.wonder.ahk.transpilers.common_x64.macros;

import fr.wonder.ahk.transpilers.common_x64.instructions.Instruction;

public class StringDefinition implements Instruction {

	public final String label, text;
	
	public StringDefinition(String label, String text) {
		this.label = label;
		this.text = text;
	}
	
	@Override
	public String toString() {
		return "def_string " + label + ",`" + text + "`";
	}
}
