package fr.wonder.ahk.compiled.expressions;

import java.util.Objects;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.types.Operation;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class OperationExp extends Expression {
	
	public final Operator operator;
	
	/** set by the linker usin {@link #setOperation(Operation)} */
	public Operation operation;
	
	public OperationExp(UnitSource source, int sourceStart, int sourceStop, Operator operator,
			Expression leftOperand, Expression rightOperand) {
		super(source, sourceStart, sourceStop, rightOperand, leftOperand);
		this.operator = operator;
	}
	
	public OperationExp(UnitSource source, int sourceStart, int sourceStop, Operator operator,
			Expression rightOperand) {
		super(source, sourceStart, sourceStop, rightOperand);
		this.operator = operator;
	}
	
	// Beware that the left operand is stored at index 1 (if it exists) and the right operand at index 0
	
	public Expression getLeftOperand() {
		return expressions.length == 1 ? null : expressions[1];
	}
	
	public Expression getRightOperand() {
		return expressions[0];
	}
	
	public void setOperation(Operation op) {
		this.operation = op;
		if(op != Operation.NOOP) {
			if(getLOType() != op.getOperandsTypes()[0])
				expressions[1] = new ConversionExp(getSource(), getLeftOperand(), op.getOperandsTypes()[0], true);
			if(getROType() != operation.getOperandsTypes()[1])
				expressions[0] = new ConversionExp(getSource(), getRightOperand(), op.getOperandsTypes()[1], true);
		}
	}
	
	/** get left operand type, null if the expression does not have a left operand */
	public VarType getLOType() {
		return getLeftOperand() == null ? null : getLeftOperand().getType();
	}
	
	public VarType getROType() {
		return getRightOperand() == null ? null : getRightOperand().getType();
	}
	
	@Override
	public String toString() {
		return "("+Objects.toString(getLeftOperand())+") "+operator+" ("+Objects.toString(getRightOperand())+")";
	}
	
	@Override
	protected VarType getValueType(TypesTable typesTable, ErrorWrapper errors) {
		return operation.getResultType();
	}

	public String operationString() {
		return getLOType() + " " + operator + " " + getROType();
	}
	
}
