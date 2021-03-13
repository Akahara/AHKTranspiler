package fr.wonder.ahk.transpilers.python;

import fr.wonder.ahk.compiled.expressions.ArrayExp;
import fr.wonder.ahk.compiled.expressions.ConversionExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.FunctionCallExp;
import fr.wonder.ahk.compiled.expressions.FunctionExp;
import fr.wonder.ahk.compiled.expressions.IndexingExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.BoolLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.FloatLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.StrLiteral;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.SizeofExp;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarNativeType;
import fr.wonder.commons.exceptions.ErrorWrapper;

class ExpressionWriter {

	static void writeExpression(Expression exp, StringBuilder sb, ErrorWrapper errors) {
		if(exp instanceof StrLiteral) {
			sb.append('"' + ((StrLiteral)exp).value + '"');
			
		} else if(exp instanceof BoolLiteral) {
			sb.append(((BoolLiteral)exp).value ? "True" : "False");

		} else if(exp instanceof IntLiteral || exp instanceof FloatLiteral) {
			sb.append(((LiteralExp<?>) exp).value);
			
		} else if(exp instanceof VarExp) {
			sb.append(((VarExp) exp).variable);
			
		} else if(exp instanceof OperationExp) {
			OperationExp o = (OperationExp) exp;
			if(o.getLeftOperand() != null)
				writeExpression(o.getLeftOperand(), sb, errors);
			sb.append(getOperatorString(o.operator));
			writeExpression(o.getRightOperand(), sb, errors);
			
		} else if(exp instanceof FunctionExp) {
			FunctionExp f = (FunctionExp) exp;
			sb.append(f.function.getSignature().declaringUnitName + "." + f.function.getSignature().computedSignature);
			sb.append("(");
			writeExpressions(f.getArguments(), sb, errors);
			sb.append(")");
			
		} else if(exp instanceof FunctionCallExp) {
			FunctionCallExp f = (FunctionCallExp) exp;
			writeExpression(f.getFunction(), sb, errors);
			sb.append("(");
			writeExpressions(f.getArguments(), sb, errors);
			sb.append(")");
			
		} else if(exp instanceof IndexingExp) {
			IndexingExp i = (IndexingExp) exp;
			writeExpression(i.getArray(), sb, errors);
			for(Expression index : i.getIndices()) {
				sb.append("[");
				writeExpression(index, sb, errors);
				sb.append("]");
			}
			
		} else if(exp instanceof ConversionExp) {
			ConversionExp c = (ConversionExp) exp;
			if(!c.isImplicit && c.castType instanceof VarNativeType) {
				sb.append(c.castType.getName());
				sb.append('(');
				writeExpression(c.getValue(), sb, errors);
				sb.append(')');
			} else {
				writeExpression(c.getValue(), sb, errors);
			}
			
		} else if(exp instanceof SizeofExp) {
			SizeofExp s = (SizeofExp) exp;
			sb.append("len(");
			writeExpression(s.getExpression(), sb, errors);
			sb.append(")");
			
		} else if(exp instanceof ArrayExp) {
			ArrayExp a = (ArrayExp) exp;
			sb.append("[");
			for(Expression e : a.getExpressions()) {
				writeExpression(e, sb, errors);
				sb.append(", ");
			}
			if(a.getExpressions().length != 0)
				sb.delete(sb.length()-2, sb.length());
			sb.append("]");
			
		} else {
			errors.add("Unkown expression type " + exp.getClass().getSimpleName() + " " + exp.getErr());
		}
	}
	
	private static void writeExpressions(Expression[] expressions, StringBuilder sb, ErrorWrapper errors) {
		for(int i = 0; i < expressions.length; i++) {
			writeExpression(expressions[i], sb, errors);
			if(i != expressions.length-1)
				sb.append(", ");
		}
	}
	
	private static String getOperatorString(Operator o) {
		switch(o) {
		case ADD:		return "+";
		case SUBSTRACT:	return "-";
		case MULTIPLY:	return "*";
		case DIVIDE:	return "/";
		case MOD:		return "%";
		case NOT:		return "!";
		case EQUALS:	return "==";
		case SEQUALS:	return "==";
		case NEQUALS:	return "!=";
		case GEQUALS:	return ">=";
		case GREATER:	return ">";
		case LEQUALS:	return "<=";
		case LOWER:		return "<";
		}
		throw new IllegalStateException("Unknown operator " + o);
	}

}
