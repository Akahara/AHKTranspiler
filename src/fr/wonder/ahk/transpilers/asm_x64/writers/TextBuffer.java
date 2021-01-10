package fr.wonder.ahk.transpilers.asm_x64.writers;

public class TextBuffer {
	
	private final StringBuilder sb = new StringBuilder();
	
	public void append(String s) {
		sb.append(s);
	}
	
	public void appendLine(String l) {
		sb.append(l);
		sb.append('\n');
	}
	
	public void appendLine() {
		sb.append('\n');
	}
	
	/** writes two spaces before the line and adds the newline character at the end */
	public void writeLine(String l) {
		sb.append("  ");
		sb.append(l);
		sb.append('\n');
	}
	
	@Override
	public String toString() {
		return sb.toString();
	}
	
}
