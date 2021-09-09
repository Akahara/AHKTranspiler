package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;

public class SizeofExp extends Expression {

	public SizeofExp(SourceReference sourceRef, Expression exp) {
		super(sourceRef, exp);
		this.type = VarType.INT;
	}
	
	public Expression getExpression() {
		return expressions[0];
	}

	@Override
	public String toString() {
		return "sizeof(" + getExpression() + ")";
	}

	public static boolean isMesurableType(VarType mesured) {
		return mesured instanceof VarArrayType;
	}
	
}
