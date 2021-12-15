package fr.wonder.ahk.transpilers.asm_x64.natives;

public enum OSInstrinsic {
	
	/** Syscalls numbers can be found at /usr/include/asm/unistd_64.h on a linux distro */
	UNIX(
			60,	// exit
			1,	// write
			12  // brk (ram usage extension)
	),
	
	MAC(
			0x02000001,	// exit
			0x02000004,	// write
			0//unimplemented
	)
	;
	
	public final int
			SYSCALL_WRITE,
			SYSCALL_EXIT,
			SYSCALL_BRK;
	
	private OSInstrinsic(int SC_EXIT, int SC_WRITE, int SC_BRK) {
		this.SYSCALL_WRITE = SC_WRITE;
		this.SYSCALL_EXIT = SC_EXIT;
		this.SYSCALL_BRK = SC_BRK;
	}

	public static OSInstrinsic getOS(String os) {
		for(OSInstrinsic o : values())
			if(o.name().equalsIgnoreCase(os))
				return o;
		return null;
	}
	
}
