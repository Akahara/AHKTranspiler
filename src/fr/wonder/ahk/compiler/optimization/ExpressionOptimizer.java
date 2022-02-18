package fr.wonder.ahk.compiler.optimization;

import fr.wonder.ahk.compiled.expressions.ConversionExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.BoolLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.FloatLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.StrLiteral;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.SimpleLambdaExp;
import fr.wonder.ahk.compiled.expressions.types.VarNativeType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.linker.ExpressionHolder;
import fr.wonder.ahk.compiler.types.NativeOperation;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnreachableException;

public class ExpressionOptimizer {
	
	/**
	 * Finds expressions that are immediately optimizable and inlines them.
	 * <p>
	 * For example <code>int:(3.4)</code> will be replaced by <code>3</code>, ie:
	 * a ConversionExp will be replaced by an IntLiteral.
	 * 
	 * @param holder the object possibly containing optimizable expressions
	 */
	public static void optimizeExpressions(ExpressionHolder holder, ErrorWrapper errors) {
		Expression[] expressions = holder.getExpressions();
		for(int i = 0; i < expressions.length; i++) {
			optimizeExpressions(expressions[i], errors);
			Expression expOptimized = optimizeExpression(expressions[i], errors);
			if(expOptimized != null)
				expressions[i] = expOptimized;
		}
	}
	
	private static LiteralExp<?> optimizeExpression(Expression raw, ErrorWrapper errors) {
		if(raw instanceof OperationExp)
			return optimizeOperationExp((OperationExp) raw, errors);
		if(raw instanceof ConversionExp)
			return optimizeConversionExp((ConversionExp) raw, errors);
		if(raw instanceof SimpleLambdaExp) {
			// lambda *expressions* cannot be optimized, however their function can be optimized
			optimizeExpressions(((SimpleLambdaExp) raw).lambda, errors);
			return null;
		}
		
		return null;
	}
	
	private static Number numberFromLiteral(LiteralExp<?> literal) {
		if(literal instanceof BoolLiteral)
			return ((Boolean) literal.value) ? 1 : 0;
		return (Number) literal.value;
		
	}
	
	private static LiteralExp<?> optimizeOperationExp(OperationExp exp, ErrorWrapper errors) {
		if(!(exp.getLeftOperand() == null || exp.getLeftOperand() instanceof LiteralExp<?>) || !(exp.getRightOperand() instanceof LiteralExp<?>))
			return null;
		
		Object l = exp.getLeftOperand() == null ? null : ((LiteralExp<?>) exp.getLeftOperand()).value;
		VarType lt = exp.getLOType();
		Object r = ((LiteralExp<?>) exp.getRightOperand()).value;
		VarType rt = exp.getROType();
		Operator o = exp.operator;
		
		if(!(rt instanceof VarNativeType) || (lt != null && !(lt instanceof VarNativeType)))
			return null;
		
		NativeOperation op = NativeOperation.getOperation((VarNativeType) lt, (VarNativeType) rt, o, true);
		if(op == null) return null;
		
		// take care of special types (only string actually)
		if(op == NativeOperation.STR_ADD_STR)
			return new StrLiteral(exp.sourceRef, l.toString() + r.toString());
		
		Number ln = l instanceof Boolean ? ((Boolean)l ? 1 : 0) : (Number) l;
		Number rn = r instanceof Boolean ? ((Boolean)r ? 1 : 0) : (Number) r;
		boolean isFloatPrecision = op.loType == VarType.FLOAT;
		boolean isIntPrecision = op.loType == VarType.INT;
		
		// take care of single operand operations
		if(op == NativeOperation.NOT_BOOL)
			return new BoolLiteral(exp.sourceRef, !(Boolean)r);
		else if(op == NativeOperation.NEG_INT)
			return new IntLiteral(exp.sourceRef, -rn.longValue());
		else if(op == NativeOperation.NEG_FLOAT)
			return new FloatLiteral(exp.sourceRef, -rn.doubleValue());
		
		switch(o) {
		case ADD: return isFloatPrecision ? 
				new FloatLiteral(exp.sourceRef, ln.doubleValue() + rn.doubleValue()) :
				new IntLiteral(exp.sourceRef, ln.longValue() + rn.longValue());
		case SUBSTRACT: return isFloatPrecision ? 
				new FloatLiteral(exp.sourceRef, ln.doubleValue() - rn.doubleValue()) :
				new IntLiteral(exp.sourceRef, ln.longValue() - rn.longValue());
		case DIVIDE: return isFloatPrecision ? 
				new FloatLiteral(exp.sourceRef, ln.doubleValue() / rn.doubleValue()) :
				new IntLiteral(exp.sourceRef, ln.longValue() / rn.longValue());
		case MOD: return isFloatPrecision ? 
				new FloatLiteral(exp.sourceRef, ln.doubleValue() % rn.doubleValue()) :
				new IntLiteral(exp.sourceRef, ln.longValue() % rn.longValue());
		case MULTIPLY: return isFloatPrecision ? 
				new FloatLiteral(exp.sourceRef, ln.doubleValue() * rn.doubleValue()) :
				new IntLiteral(exp.sourceRef, ln.longValue() * rn.longValue());
		case POWER: return isFloatPrecision ?
				new FloatLiteral(exp.sourceRef, Math.pow(ln.doubleValue(), rn.doubleValue())) :
				new IntLiteral(exp.sourceRef, (long) Math.pow(ln.longValue(), rn.longValue()));
		case SHL: return new IntLiteral(exp.sourceRef, ln.longValue() << rn.longValue());
		case SHR: return new IntLiteral(exp.sourceRef, ln.longValue() >> rn.longValue());
		case EQUALS: return new BoolLiteral(exp.sourceRef,
				isFloatPrecision ? ln.doubleValue() == rn.doubleValue() : ln.longValue() == rn.longValue());
		case NEQUALS: return new BoolLiteral(exp.sourceRef,
				isFloatPrecision ? ln.doubleValue() != rn.doubleValue() : ln.longValue() != rn.longValue());
		case GEQUALS: return new BoolLiteral(exp.sourceRef,
				isFloatPrecision ? ln.doubleValue() >= rn.doubleValue() : ln.longValue() >= rn.longValue());
		case GREATER: return new BoolLiteral(exp.sourceRef,
				isFloatPrecision ? ln.doubleValue() > rn.doubleValue() : ln.longValue() > rn.longValue());
		case LEQUALS: return new BoolLiteral(exp.sourceRef,
				isFloatPrecision ? ln.doubleValue() <= rn.doubleValue() : ln.longValue() <= rn.longValue());
		case LOWER: return new BoolLiteral(exp.sourceRef,
				isFloatPrecision ? ln.doubleValue() < rn.doubleValue() : ln.longValue() < rn.longValue());
		case STRICTEQUALS: return new BoolLiteral(exp.sourceRef,
				isFloatPrecision ? ln.doubleValue() == rn.doubleValue() : ln.longValue() == rn.longValue());
		case AND: return new BoolLiteral(exp.sourceRef, ln.longValue() != 0 && rn.longValue() != 0);
		case OR: return new BoolLiteral(exp.sourceRef, ln.longValue() != 0 || rn.longValue() != 0);
		case BITWISE_AND: return isIntPrecision ?
				new IntLiteral(exp.sourceRef, ln.longValue() & rn.longValue()) :
				new BoolLiteral(exp.sourceRef, (ln.longValue() & rn.longValue()) != 0);
		case BITWISE_OR: return isIntPrecision ?
				new IntLiteral(exp.sourceRef, ln.longValue() | rn.longValue()) :
				new BoolLiteral(exp.sourceRef, (ln.longValue() | rn.longValue()) != 0);
		case NOT:
			throw new UnreachableException();
		}
		
		return null;
	}
	
	private static LiteralExp<?> optimizeConversionExp(ConversionExp exp, ErrorWrapper errors) {
		VarType castType = exp.castType;
		if(!(exp.getValue() instanceof LiteralExp<?>) || !(castType instanceof VarNativeType))
			return null;
		if(castType == exp.getValue().getType())
			return (LiteralExp<?>) exp.getValue();
		LiteralExp<?> literal = (LiteralExp<?>) exp.getValue();
		if(castType == VarType.STR)
			return new StrLiteral(literal.sourceRef, literal.value.toString());
		if(literal instanceof StrLiteral)
			return null;
		Number n = numberFromLiteral(literal);
		if(castType == VarType.FLOAT)
			return new FloatLiteral(literal.sourceRef, n.doubleValue());
		else if(castType == VarType.INT)
			return new FloatLiteral(literal.sourceRef, n.longValue());
		
		throw new UnreachableException("Unknown conversion: " + exp);
	}
}
