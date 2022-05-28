package fr.wonder.ahk.transpilers.asm_x64.writers.operations;

import java.util.Objects;

import fr.wonder.ahk.compiled.expressions.types.VarType;

public class NativeConversion {

	public final VarType from, to;
	
	public NativeConversion(VarType from, VarType to) {
		this.from = from;
		this.to = to;
	}
	
	@Override
	public String toString() {
		return from + " -> " + to;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof NativeConversion &&
				((NativeConversion) obj).from.equals(from) &&
				((NativeConversion) obj).to.equals(to);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(from, to);
	}
	
}
