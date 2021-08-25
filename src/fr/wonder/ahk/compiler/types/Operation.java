package fr.wonder.ahk.compiler.types;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;

/**
 * An operation takes a right and possibly a left operand and return a value,
 * native operations are stored in the {@link NativeOperation} class and
 * user-defined operations (operator overloading) are declared in structures.
 */
public abstract class Operation {
	
	public final VarType loType, roType, resultType;
	public final Operator operator;
	
	public Operation(VarType loType, VarType roType, Operator operator, VarType resultType) {
		this.loType = loType;
		this.roType = roType;
		this.operator = operator;
		this.resultType = resultType;
	}

	public int argCount() {
		return hasLeftOperand() ? 2 : 1;
	}

	public boolean hasLeftOperand() {
		return loType != null;
	}
	
	@Override
	public String toString() {
		return loType + " " + operator + " " + roType + " = " + resultType;
	}

}
