package fr.wonder.ahk.transpilers.asm_x64.natives;

public enum OSInstrinsic {
	
	LINUX;
	
	public static OSInstrinsic getOS(String os) {
		for(OSInstrinsic o : values())
			if(o.name().equalsIgnoreCase(os))
				return o;
		return null;
	}
	
}
