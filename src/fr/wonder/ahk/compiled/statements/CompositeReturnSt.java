package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.utils.Utils;

public class CompositeReturnSt extends Statement {

	public CompositeReturnSt(SourceReference sourceRef, Expression[] returnValues) {
		super(sourceRef, returnValues);
	}
	
	@Override
	public String toString() {
		return "return " + Utils.toString(expressions);
	}
	
}
