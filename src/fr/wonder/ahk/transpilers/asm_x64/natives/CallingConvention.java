package fr.wonder.ahk.transpilers.asm_x64.natives;

public enum CallingConvention {
	
	/**
	 * Arguments are passed onto the stack, the right-most one at the higher address
	 * (near the stack begin). The called function cleans the stack.
	 */
	__stdcall;

	public static CallingConvention getConvention(String c) {
		for(CallingConvention o : values())
			if(o.name().equalsIgnoreCase(c))
				return o;
		return null;
	}
	
}
