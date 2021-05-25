package fr.wonder.ahk.transpilers.common_x64.addresses;

import fr.wonder.ahk.transpilers.common_x64.MemSize;
import fr.wonder.ahk.transpilers.common_x64.instructions.OperationParameter;

public class ImmediateValue implements OperationParameter {

	public final String text;
	public final MemSize cast;
	
	public ImmediateValue(String text, MemSize cast) {
		this.text = text;
		this.cast = cast;
	}
	
	public ImmediateValue(String text) {
		this(text, null);
	}
	
	public ImmediateValue(int val, MemSize cast) {
		this(String.valueOf(val), cast);
	}
	
	public ImmediateValue(int val) {
		this(val, null);
	}
	
	@Override
	public String toString() {
		return text;
	}
}
