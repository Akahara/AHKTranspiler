package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarType;

/**
 * Ranged for statements can be written as
 * "<code>for(int i : a..b(..c)):</code>"<br>
 * Note that {@code b} must be fixed (an integer literal) and that {@code c}
 * will be evaluated once each pass. Note again that <code>sizeof(array)</code>
 * is not fixed! the {@code array} reference may be
 * modified inside the for loop.
 */
public class RangedForSt extends LabeledStatement {

	public final String variable;
	
	public RangedForSt(UnitSource source, int sourceStart, int sourceStop, boolean singleLine,
			String varName, Expression min, Expression max, Expression step) {
		super(source, sourceStart, sourceStop, singleLine, min, max, step);
		this.variable = varName;
	}
	
	public VariableDeclaration getVariableDeclaration() {
		return new VariableDeclaration(getSource(), sourceStart, sourceStop, variable, VarType.INT, getMin());
	}
	
	public Expression getMin() {
		return expressions[0];
	}
	
	public Expression getMax() {
		return expressions[1];
	}
	
	public Expression getStep() {
		return expressions[2];
	}
	
	public ForSt toComplexFor() {
		return new ForSt(getSource(), sourceStart, sourceStop, singleLine,
				getVariableDeclaration(),
				new OperationExp(getSource(), sourceStart, sourceStop, Operator.LOWER,
						new VarExp(getSource(), sourceStart, sourceStop, variable),
						getMax()),
				new AffectationSt(getSource(), sourceStart, sourceStop,
						new VarExp(getSource(), sourceStart, sourceStop, variable), 
						new OperationExp(getSource(), sourceStart, sourceStop, Operator.ADD,
								new VarExp(getSource(), sourceStart, sourceStop, variable),
								getStep())));
	}
	
	@Override
	public String toString() {
		return "for(int " + variable + " : " + getMin() + " .. " + getMax() + " .. " + getStep() + ")";
	}

}
