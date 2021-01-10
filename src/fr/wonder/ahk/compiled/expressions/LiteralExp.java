package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.ahk.utils.ErrorWrapper;

public abstract class LiteralExp<T> extends Expression {
	
	public final T value;
	
	private LiteralExp(Unit unit, int sourceStart, int sourceStop, T value) {
		super(unit, sourceStart, sourceStop);
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
		
		public IntLiteral(Unit unit, int sourceStart, int sourceStop, long i) {
			super(unit, sourceStart, sourceStop, i);
		}
		
		@Override
		protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
			return VarType.INT;
		}
		
	}
	
	public static class FloatLiteral extends LiteralExp<Float> {
		
		public FloatLiteral(Unit unit, int sourceStart, int sourceStop, float f) {
			super(unit, sourceStart, sourceStop, f);
		}

		@Override
		protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
			return VarType.FLOAT;
		}
		
	}
	
	public static class BoolLiteral extends LiteralExp<Boolean> {
		
		public BoolLiteral(Unit unit, int sourceStart, int sourceStop, boolean b) {
			super(unit, sourceStart, sourceStop, b);
		}

		@Override
		protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
			return VarType.BOOL;
		}
		
	}
	
	public static class StrLiteral extends LiteralExp<String> {
		
		public StrLiteral(Unit unit, int sourceStart, int sourceStop, String s) {
			super(unit, sourceStart, sourceStop, s);
		}

		@Override
		protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
			return VarType.STR;
		}
		
		@Override
		public String toString() {
			return '"'+value+'"';
		}
	}
	
}
