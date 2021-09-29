package fr.wonder.ahk.compiled.expressions;

import java.util.Objects;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.types.Operation;

public class OperationExp extends Expression {
	
	public final Operator operator;
	
	/** set by the linker using {@link #setOperation(Operation)} */
	private Operation operation;
	
	public OperationExp(SourceReference sourceRef, Operator operator,
			Expression leftOperand, Expression rightOperand) {
		super(sourceRef, rightOperand, leftOperand);
		this.operator = operator;
	}
	
	public OperationExp(SourceReference sourceRef, Operator operator,
			Expression rightOperand) {
		super(sourceRef, rightOperand);
		this.operator = operator;
	}
	
	// Beware that the left operand is stored at index 1 (if it exists) and the right operand at index 0
	
	public Expression getLeftOperand() {
		return expressions.length == 1 ? null : expressions[1];
	}
	
	public Expression getRightOperand() {
		return expressions[0];
	}
	
	/** Returns an unmodifiable array of this operation's operands */
	public Expression[] getOperands() {
		// if there are two operands, swap their orders (the left operand is stored at index 1)
		return expressions.length == 1 ? expressions :
			new Expression[] { expressions[1], expressions[0] };
	}
	
	/**
	 * This method will return an array containing this operation's operands
	 * <b>from right to left</b>, use {@link #getOperands()} to get theim in
	 * the natural order
	 */
	@Override
	public Expression[] getExpressions() {
		return super.getExpressions();
	}
	
	public void setOperation(Operation op) {
		this.operation = op;
		if(op == Invalids.OPERATION)
			return;
		if((op.loType == null) != (getLOType() == null))
			throw new IllegalStateException("Invalid operand count");
		if(!Objects.equals(getLOType(), op.loType))
			expressions[1] = new ConversionExp(getLeftOperand(), op.loType);
		if(!getROType().equals(op.roType))
			expressions[0] = new ConversionExp(getRightOperand(), op.roType);
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
	
	public String operationString() {
		return getLOType() + " " + operator + " " + getROType();
	}

}
