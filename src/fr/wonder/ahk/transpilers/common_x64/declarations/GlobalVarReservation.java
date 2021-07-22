package fr.wonder.ahk.transpilers.common_x64.declarations;

import fr.wonder.ahk.transpilers.common_x64.MemSize;

public class GlobalVarReservation implements Declaration {

	public final String label;
	public final MemSize size;
	public final int count;
	
	public GlobalVarReservation(String label, MemSize size, int count) {
		this.label = label;
		this.size = size;
		this.count = count;
	}
	
	@Override
	public String toString() {
		return label + " " + size.reservation + " " + count;
	}
	
}
