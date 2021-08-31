package fr.wonder.ahk.transpilers.ahl.raw;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.FunctionExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.transpilers.ahl.values.LiteralValue;
import fr.wonder.ahk.transpilers.ahl.values.Value;
import fr.wonder.commons.exceptions.UnimplementedException;

public class ExpressionWriter {
	
	private final FunctionWriter func;
	
	public ExpressionWriter(FunctionWriter func) {
		this.func = func;
	}
	
	public Value writeExpression(Expression exp) {
		if(exp instanceof LiteralExp<?>) {
			return new LiteralValue(exp.getType(), ((LiteralExp<?>) exp).value);
		} else if(exp instanceof FunctionExp) {
			func.instructions.vPointer();
		} else {
			throw new UnimplementedException("Unknown expression type: " + exp.getClass());
		}
	}

}
