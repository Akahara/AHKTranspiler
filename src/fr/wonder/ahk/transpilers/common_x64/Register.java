package fr.wonder.ahk.transpilers.common_x64;

import fr.wonder.ahk.transpilers.common_x64.addresses.Address;

public class Register implements Address {

	/** Standard register */
	public static final Register
		RAX = new Register("rax"),
		RBX = new Register("rbx"),
		RCX = new Register("rcx"),
		RDX = new Register("rdx");
	
	/** Lower 8 bits of the RAX register */
	public static final Register AL = new Register("al");
	
	/** Source register */
	public static final Register RSI = new Register("rsi");
	/** Destination register */
	public static final Register RDI = new Register("rdi");
	
	/** Stack pointer */
	public static final Register RSP = new Register("rsp");
	/** Base pointer (function-scope stack start) */
	public static final Register RBP = new Register("rbp");

	/** Floating point unit (FPU) register */
	public static final Register
		ST0 = new Register("st0"),
		ST1 = new Register("st1"),
		ST2 = new Register("st2");

	public final String name;
	
	private Register(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
}
