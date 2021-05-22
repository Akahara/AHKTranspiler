package fr.wonder.ahk.transpilers.common_x64;

import fr.wonder.ahk.transpilers.common_x64.addresses.Address;

public class Register implements Address {

	public static final Register RAX = new Register("rax");
	public static final Register RBX = new Register("rbx");
	public static final Register RCX = new Register("rcx");
	public static final Register RDX = new Register("rdx");
	
	public final String name;
	
	private Register(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
}
