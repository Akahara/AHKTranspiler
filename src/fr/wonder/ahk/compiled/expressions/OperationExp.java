package fr.wonder.ahk.compiled.expressions;

import java.util.Objects;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.types.Operation;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class OperationExp extends Expression {
	
	public final Operator operator;
	
	/** set by the linker using {@link #setOperation(Operation)} */
	private Operation operation;
	
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
		if(op == Invalids.OPERATION)
			return;
		if((op.getLOType() == null) != (getLOType() == null))
			throw new IllegalStateException("Invalid operand count");
		if(getLOType() != op.getLOType())
			expressions[1] = new ConversionExp(getLeftOperand(), op.getLOType());
		if(getROType() != op.getROType())
			expressions[0] = new ConversionExp(getRightOperand(), op.getROType());
	}
	
	public Operation getOperation() {
		return operation;
	}
	
	/** get left operand type, null if the expression does not have a left operand */
	public VarType getLOType() {
		return getLeftOperand() == null ? null : getLeftOperand().getType();
	}
	
	public VarType getROType() {
		return getRightOperand().getType();
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
