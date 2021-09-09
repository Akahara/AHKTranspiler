package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiler.types.NativeOperation;

/**
 * Ranged for statements can be written as
 * "<code>for(int i : a..b(..c)):</code>"<br>
 * Note that {@code b} must be fixed (an integer literal) and that {@code c}
 * will be evaluated once each pass. Note again that <code>sizeof(array)</code>
 * is not fixed! the {@code array} reference may be
 * modified inside the for loop.
 */
public class RangedForSt extends LabeledStatement {

	private final VariableDeclaration variable;
	
	public RangedForSt(Unit unit, SourceReference sourceRef, boolean singleLine,
			String varName, Expression min, Expression max, Expression step) {
		super(sourceRef, singleLine, min, max, step);
		this.variable = new VariableDeclaration(unit, sourceRef,
				varName, VarType.INT, DeclarationModifiers.NONE, getMin());
	}

	public VariableDeclaration getVariableDeclaration() {
		return variable;
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
		VarExp var = new VarExp(sourceRef, variable.name);
		var.declaration = variable.getPrototype();
		var.type = variable.getType();
		OperationExp condition = new OperationExp(sourceRef, Operator.LOWER, var, getMax());
		condition.setOperation(NativeOperation.getOperation(VarType.INT, VarType.INT, Operator.LOWER, false));
		OperationExp affectationValue = new OperationExp(sourceRef, Operator.ADD, var, getStep());
		affectationValue.setOperation(NativeOperation.getOperation(VarType.INT, VarType.INT, Operator.ADD, false));
		AffectationSt affectation = new AffectationSt(sourceRef, var, affectationValue);
		ForSt st = new ForSt(sourceRef, singleLine, variable, condition, affectation);
		st.sectionEnd = this.sectionEnd;
		return st;
	}

	@Override
	public String toString() {
		return "for(int " + variable + " : " + getMin() + " .. " + getMax() + " .. " + getStep() + ")";
	}

}
