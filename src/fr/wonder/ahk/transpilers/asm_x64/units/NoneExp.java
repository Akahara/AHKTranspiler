package fr.wonder.ahk.transpilers.asm_x64.units;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.transpilers.asm_x64.writers.MemoryManager;

/**
 * Used by the {@link MemoryManager} to write <i>null</i> as a default value for variable declarations without default value
 */
public class NoneExp extends Expression {
	
	private static class NoneType extends VarType {
		
		private static final NoneType INSTANCE = new NoneType();
		
		public String getName() {
			throw new IllegalStateException();
		}
		public String getSignature() {
			throw new IllegalStateException();
		}
		public VarType[] getSubTypes() {
			throw new IllegalStateException();
		}
		public boolean equals(Object o) {
			throw new IllegalAccessError("The None type cannot be compared");
		}
	}
	
	/** @see NoneExp */
	public NoneExp() {
		super(Invalids.SOURCE_REF);
		this.type = NoneType.INSTANCE;
	}

	@Override
	public String toString() {
		return "none";
	}
	
}
