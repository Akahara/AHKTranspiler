package fr.wonder.ahk.transpilers.common_x64.declarations;

import fr.wonder.ahk.transpilers.common_x64.MemSize;

public class GlobalVarDeclaration implements Declaration {
	
	public final String label;
	public final MemSize size;
	public final String value;
	
	public GlobalVarDeclaration(String label, MemSize size, String value) {
		this.label = label;
		this.size = size;
		this.value = value;
	}
	
	public GlobalVarDeclaration(MemSize size, String value) {
		this(" ", size, value);
	}
	
	@Override
	public String toString() {
		return label + " " + size.declaration + " " + value;
	}
	
}
