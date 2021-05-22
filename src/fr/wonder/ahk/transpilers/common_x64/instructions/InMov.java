package fr.wonder.ahk.transpilers.common_x64.instructions;

import fr.wonder.ahk.transpilers.common_x64.addresses.Address;

public class InMov implements Instruction {
	
	public final Address from, to;
	
	public InMov(Address from, Address to) {
		this.from = from;
		this.to = to;
	}
	
}
