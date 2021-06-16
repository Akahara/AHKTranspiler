package fr.wonder.ahk.transpilers.asm_x64.writers;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

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
		
		@Override
		public boolean equals(Object o) {
			throw new IllegalAccessError("The None type cannot be compared");
		}
	}
	
	/** The size (in bytes) of the "none" value */
	public final int size;
	
	/** @see NoneExp */
	public NoneExp(int size) {
		super(null, 0, 0);
		this.size = size;
		this.type = NoneType.INSTANCE;
	}

	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		return type;
	}
	
	@Override
	public String toString() {
		return "none";
	}
	
}
