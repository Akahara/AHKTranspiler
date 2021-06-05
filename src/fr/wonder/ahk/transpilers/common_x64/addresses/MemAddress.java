package fr.wonder.ahk.transpilers.common_x64.addresses;

import fr.wonder.ahk.transpilers.common_x64.Register;
import fr.wonder.commons.utils.Assertions;

public class MemAddress implements Address {
		
	public final Address base;
	public final Register index;
	public final int scale, offset;
	
	private final String stringResp;
	
	public MemAddress(Address base, Register index, int scale, int offset) {
		this.base = base;
//		Assertions.assertTrue(base instanceof LabelAddress || base instanceof Register, "Invalid base " + base);
		Assertions.assertFalse(index != null && scale == 0, "Scale specified without displacement");
		this.index = index;
		this.scale = scale;
		this.offset = offset;
		String repr = base.toString();
		if(index != null) {
			repr += scale < 0 ? scale+"*" : "+"+scale+"*";
			repr += index.toString();
		}
		if(offset != 0)
			repr += offset < 0 ? offset+"" : "+"+offset+"";
		this.stringResp = "["+repr+"]";
	}
	
	public MemAddress(Address base, int offset) {
		this(base, null, 0, offset);
	}

	public MemAddress(Address base) {
		this(base, null, 0, 0);
	}
	
	public MemAddress(Address base, Register index, int scale) {
		this(base, index, scale, 0);
	}

	/** Returns [this] */
	public MemAddress dereference() {
		return new MemAddress(this);
	}
	/** Returns [[this]+...] */
	public MemAddress then(Register index, int scale, int offset) {
		return new MemAddress(this, index, scale, offset);
	}
	
	/** Returns [[this]+...] */
	public MemAddress then(int offset) {
		return new MemAddress(this, offset);
	}
	/** Returns [[this]+...] */
	public MemAddress then(Register index, int scale) {
		return new MemAddress(this, index, scale);
	}

	/** Returns a <b>new instance</b> of MemAddress with same base, index and scale but cumulated offsets */
	public MemAddress addOffset(int offset) {
		return new MemAddress(base, index, scale, this.offset+offset);
	}
	
	@Override
	public String toString() {
		return stringResp;
	}
	
}
