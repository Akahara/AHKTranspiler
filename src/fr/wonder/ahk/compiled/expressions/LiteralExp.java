package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;

public abstract class LiteralExp<T> extends Expression {
	
	public final T value;
	
	protected LiteralExp(SourceReference sourceRef, VarType type, T value) {
		super(sourceRef);
		this.type = type;
		this.value = value;
	}
	
	@Override
	public String toString() {
		return String.valueOf(value);
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof LiteralExp && value.equals(((LiteralExp<?>)o).value);
	}
	
	public static class IntLiteral extends LiteralExp<Long> {
		
		public IntLiteral(SourceReference sourceRef, long i) {
			super(sourceRef, VarType.INT, i);
		}
		
	}
	
	public static class FloatLiteral extends LiteralExp<Double> {
		
		public FloatLiteral(SourceReference sourceRef, double f) {
			super(sourceRef, VarType.FLOAT, f);
		}
		
	}
	
	public static class BoolLiteral extends LiteralExp<Boolean> {
		
		public BoolLiteral(SourceReference sourceRef, boolean b) {
			super(sourceRef, VarType.BOOL, b);
		}

	}
	
	public static class StrLiteral extends LiteralExp<String> {
		
		public StrLiteral(SourceReference sourceRef, String s) {
			super(sourceRef, VarType.STR, s);
		}

		@Override
		public String toString() {
			return '"'+value+'"';
		}
	}
	
}
