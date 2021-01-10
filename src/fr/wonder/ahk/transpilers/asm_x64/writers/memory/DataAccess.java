package fr.wonder.ahk.transpilers.asm_x64.writers.memory;

import fr.wonder.ahk.compiled.expressions.LiteralExp;

public class DataAccess {
	
	/** Either this or {@link #exp} is set, the other is null */
	public final VarLocation loc;
	/** Either this or {@link #loc} is set, the other is null */
	public final LiteralExp<?> exp;

	public DataAccess(VarLocation loc) {
		this.loc = loc;
		this.exp = null;
	}
	
	public DataAccess(LiteralExp<?> exp) {
		this.loc = null;
		this.exp = exp;
	}
	
	@Override
	public String toString() {
		// FIX non-integer literals must be considered in another way!!
		return loc == null ? exp.toString() : loc.getLoc();
	}

}
