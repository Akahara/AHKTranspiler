package fr.wonder.ahk.transpilers.python;

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
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarNativeType;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.utils.ErrorWrapper;

class ExpressionWriter {

	static void writeExpression(Unit unit, Expression exp, StringBuilder sb, ErrorWrapper errors) {
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
				writeExpression(unit, o.getLeftOperand(), sb, errors);
			sb.append(getOperatorString(o.operator));
			writeExpression(unit, o.getRightOperand(), sb, errors);
			
		} else if(exp instanceof FunctionExp) {
			FunctionExp f = (FunctionExp) exp;
			if(f.function.modifiers.hasModifier(Modifier.NATIVE))
				sb.append(f.function.name);
			else
				sb.append(f.function.declaringUnit.name + '.' + f.function.name);
			sb.append("(");
			writeExpressions(unit, f.getArguments(), sb, errors);
			sb.append(")");
			
		} else if(exp instanceof FunctionCallExp) {
			FunctionCallExp f = (FunctionCallExp) exp;
			writeExpression(unit, f.getFunction(), sb, errors);
			sb.append("(");
			writeExpressions(unit, f.getArguments(), sb, errors);
			sb.append(")");
			
		} else if(exp instanceof IndexingExp) {
			IndexingExp i = (IndexingExp) exp;
			writeExpression(unit, i.getArray(), sb, errors);
			for(Expression index : i.getIndices()) {
				sb.append("[");
				writeExpression(unit, index, sb, errors);
				sb.append("]");
			}
			
		} else if(exp instanceof ConversionExp) {
			ConversionExp c = (ConversionExp) exp;
			if(!c.isImplicit && c.castType instanceof VarNativeType) {
				sb.append(c.castType.getName());
				sb.append('(');
				writeExpression(unit, c.getValue(), sb, errors);
				sb.append(')');
			} else {
				writeExpression(unit, c.getValue(), sb, errors);
			}
			
		} else {
			errors.add("Unkown expression type " + exp.getClass().getSimpleName());
		}
	}
	
	private static void writeExpressions(Unit unit, Expression[] expressions, StringBuilder sb, ErrorWrapper errors) {
		for(int i = 0; i < expressions.length; i++) {
			writeExpression(unit, expressions[i], sb, errors);
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
		case EQUALS:	return "==";
		case SEQUALS:	return "==";
		case GEQUALS:	return ">=";
		case GREATER:	return ">";
		case LEQUALS:	return "<=";
		case LOWER:		return "<";
		}
		throw new IllegalStateException("Unknown operator " + o);
	}

}
