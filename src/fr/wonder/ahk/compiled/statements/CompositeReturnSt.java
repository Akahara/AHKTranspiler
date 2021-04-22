package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.utils.Utils;

public class CompositeReturnSt extends Statement {

	public CompositeReturnSt(UnitSource source, int sourceStart, int sourceStop, Expression[] returnValues) {
		super(source, sourceStart, sourceStop, returnValues);
	}
	
	@Override
	public String toString() {
		return "return " + Utils.toString(expressions);
	}
	
}
