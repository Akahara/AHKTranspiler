package fr.wonder.ahk.transpilers.common_x64;

import fr.wonder.ahk.transpilers.common_x64.addresses.Address;

public class Register implements Address {

	public static final Register RAX = new Register("rax");
	public static final Register RBX = new Register("rbx");
	public static final Register RCX = new Register("rcx");
	public static final Register RDX = new Register("rdx");
	
//	public static final Register EAX = new Register("eax");
	
	public static final Register RSP = new Register("rsp");
	public static final Register RBP = new Register("rbp");

	public final String name;
	public final MemSize size;
	
	private Register(String name) {
		this.name = name;
		switch(name.charAt(0)) {
		case 'r': this.size = MemSize.QWORD; break;
		case 'e': this.size = MemSize.DWORD; break;
		default: throw new IllegalStateException("Unknown size for register " + name);
		}
	}
	
	@Override
	public String toString() {
		return name;
	}
	
}
