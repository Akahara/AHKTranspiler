package fr.wonder.ahk.compiler.optimization;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.BoolLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.FloatLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.StrLiteral;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.linker.ExpressionHolder;
import fr.wonder.ahk.compiler.types.NativeOperation;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnreachableException;

public class ExpressionOptimizer {
	
	public static boolean optimizeExpressions(ExpressionHolder holder, ErrorWrapper errors) {
		Expression[] expressions = holder.getExpressions();
		boolean optimized = true;
		for(int i = 0; i < expressions.length; i++) {
			boolean expOpt = optimizeExpressions(expressions[i], errors);
			optimized &= expOpt;
			
			if(expOpt) {
				Expression expOptimized = optimizeExpression(expressions[i], errors);
				if(expOptimized == null)
					optimized = false;
				else if(expressions[i] != expOptimized)
					expressions[i] = expOptimized;
			}
		}
		
		return optimized;
	}
	
	/**
	 * Can only be called on expressions with fully optimized sub-expressions (or no
	 * sub-expressions at all). Returns null if no changes can be made, the optimized
	 * expression otherwise.
	 */
	private static LiteralExp<?> optimizeExpression(Expression raw, ErrorWrapper errors) {
		if(raw instanceof LiteralExp<?>)
			return (LiteralExp<?>) raw;
		if(raw instanceof OperationExp)
			return optimizeOperationExp((OperationExp) raw, errors);
		
		return null;
	}
	
	private static LiteralExp<?> optimizeOperationExp(OperationExp exp, ErrorWrapper errors) {
		Object l = exp.getLeftOperand() == null ? null :((LiteralExp<?>) exp.getLeftOperand()).value;
		Object r = ((LiteralExp<?>) exp.getRightOperand()).value;
		Operator o = exp.operator;
		
		NativeOperation op = NativeOperation.getOperation(exp.getLOType(), exp.getROType(), o, true);
		if(op == null) return null;
		
		// take care of non-native types (only string actually)
		if(op == NativeOperation.STR_ADD_STR)
			return new StrLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop, l.toString() + r.toString());
		
		Number ln = l instanceof Boolean ? ((Boolean)l ? 1 : 0) : (Number) l;
		Number rn = r instanceof Boolean ? ((Boolean)r ? 1 : 0) : (Number) r;
		boolean isFloatPrecision = op.loType == VarType.FLOAT;
		
		// take care of single operand operations
		if(op == NativeOperation.NOT_BOOL)
			return new BoolLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop, !(Boolean)r);
		else if(op == NativeOperation.NEG_INT)
			return new IntLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop, -rn.longValue());
		else if(op == NativeOperation.NEG_FLOAT)
			return new FloatLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop, -rn.doubleValue());
		
		
		switch(o) {
		case ADD: return isFloatPrecision ? 
				new FloatLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop, ln.doubleValue() + rn.doubleValue()) :
				new IntLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop, ln.longValue() + rn.longValue());
		case SUBSTRACT: return isFloatPrecision ? 
				new FloatLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop, ln.doubleValue() - rn.doubleValue()) :
				new IntLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop, ln.longValue() - rn.longValue());
		case DIVIDE: return isFloatPrecision ? 
				new FloatLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop, ln.doubleValue() / rn.doubleValue()) :
				new IntLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop, ln.longValue() / rn.longValue());
		case MOD: return isFloatPrecision ? 
				new FloatLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop, ln.doubleValue() % rn.doubleValue()) :
				new IntLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop, ln.longValue() % rn.longValue());
		case MULTIPLY: return isFloatPrecision ? 
				new FloatLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop, ln.doubleValue() * rn.doubleValue()) :
				new IntLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop, ln.longValue() * rn.longValue());
		case POWER:
			return null; // TODO implement the POWER operation optimization
		case SHL: return new IntLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop, ln.longValue() << rn.longValue());
		case SHR: return new IntLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop, ln.longValue() >> rn.longValue());
		case EQUALS: return new BoolLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop,
				isFloatPrecision ? ln.doubleValue() == rn.doubleValue() : ln.longValue() == rn.longValue());
		case NEQUALS: return new BoolLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop,
				isFloatPrecision ? ln.doubleValue() != rn.doubleValue() : ln.longValue() != rn.longValue());
		case GEQUALS: return new BoolLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop,
				isFloatPrecision ? ln.doubleValue() >= rn.doubleValue() : ln.longValue() >= rn.longValue());
		case GREATER: return new BoolLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop,
				isFloatPrecision ? ln.doubleValue() > rn.doubleValue() : ln.longValue() > rn.longValue());
		case LEQUALS: return new BoolLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop,
				isFloatPrecision ? ln.doubleValue() <= rn.doubleValue() : ln.longValue() <= rn.longValue());
		case LOWER: return new BoolLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop,
				isFloatPrecision ? ln.doubleValue() < rn.doubleValue() : ln.longValue() < rn.longValue());
		case STRICTEQUALS: return new BoolLiteral(exp.getSource(), exp.sourceStart, exp.sourceStop,
				isFloatPrecision ? ln.doubleValue() == rn.doubleValue() : ln.longValue() == rn.longValue());
		case NOT:
			throw new UnreachableException();
		}
		
		return null;
	}
	
}
