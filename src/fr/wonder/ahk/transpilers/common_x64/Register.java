package fr.wonder.ahk.transpilers.common_x64;

import fr.wonder.ahk.transpilers.common_x64.addresses.Address;

public class Register implements Address {

	public static final Register RAX = new Register("rax");
	public static final Register RBX = new Register("rbx");
	public static final Register RCX = new Register("rcx");
	public static final Register RDX = new Register("rdx");
	
	public static final Register AL = new Register("al");
	
	public static final Register RSI = new Register("rsi");
	public static final Register RDI = new Register("rdi");
	
	public static final Register RSP = new Register("rsp");
	public static final Register RBP = new Register("rbp");

	public final String name;
	
	private Register(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
}
