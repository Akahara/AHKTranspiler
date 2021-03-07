package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarType;

/**
 * Simple for statements are statements that can be written as
 * "<code>for(int i : a..b(..c)):</code>"<br>
 * Note that {@code b} (and {@code c} if present) must be fixed (integer literals) for the
 * for statement to be considered simple. Note again that
 * <code>sizeof(array)</code> is not fixed! the {@code array} reference may be
 * modified inside the for loop.
 */
public class SimpleForSt extends LabeledStatement {

	public final String variable;
	public final int min, max, step;
	
	public SimpleForSt(UnitSource source, int sourceStart, int sourceStop, boolean singleLine,
			String varName, int min, int max, int step) {
		super(source, sourceStart, sourceStop, singleLine);
		this.variable = varName;
		this.min = min;
		this.max = max;
		this.step = step;
	}
	
	public ForSt toComplexFor() {
		return new ForSt(getSource(), sourceStart, sourceStop, singleLine,
				new VariableDeclaration(getSource(), sourceStart, sourceStop, variable, VarType.INT),
				new OperationExp(getSource(), sourceStart, sourceStop, Operator.LOWER,
						new VarExp(getSource(), sourceStart, sourceStop, variable),
						new IntLiteral(getSource(), sourceStart, sourceStop, max)),
				new AffectationSt(getSource(), sourceStart, sourceStop,
						new VarExp(getSource(), sourceStart, sourceStop, variable), 
						new OperationExp(getSource(), sourceStart, sourceStop, Operator.ADD,
								new VarExp(getSource(), sourceStart, sourceStop, variable),
								new IntLiteral(getSource(), sourceStart, sourceStop, step))));
	}

}
