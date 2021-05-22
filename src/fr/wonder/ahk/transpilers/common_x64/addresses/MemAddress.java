package fr.wonder.ahk.transpilers.common_x64.addresses;

import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.ahk.transpilers.common_x64.declarations.Label;
import fr.wonder.commons.utils.Assertions;

public class MemAddress implements Address {
		
	public final Address base;
	public final Register index;
	public final int scale, offset;
	
	private final String stringResp;
	
	public MemAddress(Address base, Register index, int scale, int offset) {
		this.base = base;
		Assertions.assertTrue(base instanceof Label || base instanceof Register, "Invalid base " + base);
		Assertions.assertTrue(scale == 0 || index != null, "Size specified without displacement");
		this.index = index;
		this.scale = scale;
		this.offset = offset;
		String repr = base.toString();
		if(index != null) {
			if(scale != 0)
				repr += scale < 0 ? scale+"*" : "+"+scale+"*";
			repr += index.toString();
		}
		if(offset != 0)
			repr += offset < 0 ? offset+"" : "+"+offset+"";
		this.stringResp = "["+repr+"]";
	}
	
	@Override
	public String toString() {
		return stringResp;
	}
		
}
