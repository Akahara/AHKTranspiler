package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

public abstract class LiteralExp<T> extends Expression {
	
	public final T value;
	
	protected LiteralExp(SourceReference sourceRef, T value) {
		super(sourceRef);
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
			super(sourceRef, i);
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
		
		public FloatLiteral(SourceReference sourceRef, double f) {
			super(sourceRef, f);
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
		
		public BoolLiteral(SourceReference sourceRef, boolean b) {
			super(sourceRef, b);
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
		
		public StrLiteral(SourceReference sourceRef, String s) {
			super(sourceRef, s);
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
