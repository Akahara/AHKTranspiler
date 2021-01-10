package fr.wonder.ahk.transpilers.asm_x64.natives;

public enum CallingConvention {
	
	__stdcall;

	public static CallingConvention getConvention(String c) {
		for(CallingConvention o : values())
			if(o.name().equalsIgnoreCase(c))
				return o;
		return null;
	}
	
}
