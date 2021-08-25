package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

public abstract class LiteralExp<T> extends Expression {
	
	public final T value;
	
	protected LiteralExp(UnitSource source, int sourceStart, int sourceStop, T value) {
		super(source, sourceStart, sourceStop);
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
		
		public IntLiteral(UnitSource source, int sourceStart, int sourceStop, long i) {
			super(source, sourceStart, sourceStop, i);
		}
		
		@Override
		protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
			return VarType.INT;
		}
		
		@Override
		public VarType getType() {
			return VarType.INT;
		}
		
	}
	
	public static class FloatLiteral extends LiteralExp<Double> {
		
		public FloatLiteral(UnitSource source, int sourceStart, int sourceStop, double f) {
			super(source, sourceStart, sourceStop, f);
		}

		@Override
		protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
			return VarType.FLOAT;
		}
		
		@Override
		public VarType getType() {
			return VarType.FLOAT;
		}
		
	}
	
	public static class BoolLiteral extends LiteralExp<Boolean> {
		
		public BoolLiteral(UnitSource source, int sourceStart, int sourceStop, boolean b) {
			super(source, sourceStart, sourceStop, b);
		}

		@Override
		protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
			return VarType.BOOL;
		}
		
		@Override
		public VarType getType() {
			return VarType.BOOL;
		}
		
	}
	
	public static class StrLiteral extends LiteralExp<String> {
		
		public StrLiteral(UnitSource source, int sourceStart, int sourceStop, String s) {
			super(source, sourceStart, sourceStop, s);
		}

		@Override
		protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
			return VarType.STR;
		}
		
		@Override
		public VarType getType() {
			return VarType.STR;
		}
		
		@Override
		public String toString() {
			return '"'+value+'"';
		}
	}
	
}
