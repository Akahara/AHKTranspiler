package fr.wonder.ahk.compiler.types;

import fr.wonder.ahk.compiled.expressions.types.VarType;

public interface Operation {
	
	final VarType[] NOOPERANDS = {};
	
	Operation NOOP = new Operation() {
		@Override
		public VarType getResultType() {
			return VarType.NULL;
		}
		@Override
		public VarType[] getOperandsTypes() {
			return NOOPERANDS;
		}
	};

	public VarType getResultType();
	public VarType[] getOperandsTypes();
	
}
