package fr.wonder.ahk.transpilers.python;

import static fr.wonder.ahk.transpilers.python.ExpressionWriter.writeExpression;

import java.util.Arrays;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.AffectationSt;
import fr.wonder.ahk.compiled.statements.ElseSt;
import fr.wonder.ahk.compiled.statements.ForSt;
import fr.wonder.ahk.compiled.statements.FunctionSt;
import fr.wonder.ahk.compiled.statements.IfSt;
import fr.wonder.ahk.compiled.statements.ReturnSt;
import fr.wonder.ahk.compiled.statements.SectionEndSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.exceptions.ErrorWrapper;

class FunctionWriter {
	
	static void writeFunction(Unit unit, FunctionSection func, StringBuilder sb, ErrorWrapper errors) {
		sb.append("  @staticmethod\n");
		sb.append("  def " + func.getSignature().computedSignature + "(");
		sb.append(Utils.mapToString(func.arguments, arg->arg.name));
		sb.append("):\n");
		
		// write statements
		
		if(func.body.length == 0)
			sb.append("    pass\n");
		
		int indent = 2;
		boolean indentHasStatement = true;
		
		for(int i = 0; i < func.body.length; i++) {
			Statement st = func.body[i];
			if(st instanceof SectionEndSt) {
				if(!indentHasStatement)
					sb.append("  ".repeat(indent) + "pass\n");
				indent--;
				indentHasStatement = true;
				continue;
			}
			
			sb.append("  ".repeat(indent));
			
			if(st instanceof VariableDeclaration) {
				writeVarDeclaration(unit, (VariableDeclaration) st, sb, errors);
				
			} else if(st instanceof IfSt) {
				writeIfStatement(unit, (IfSt) st, sb, errors);
				
			} else if(st instanceof ElseSt) {
				writeElseStatement(unit, (ElseSt) st, sb, errors);
				
			} else if(st instanceof ForSt) {
				writeForStatement(unit, (ForSt) st, sb, errors);
				
			} else if(st instanceof ReturnSt) {
				writeReturnStatement(unit, (ReturnSt) st, sb, errors);
				
			} else if(st instanceof FunctionSt) {
				writeExpression(unit, ((FunctionSt) st).getFunction(), sb, errors);
				
			} else if(st instanceof AffectationSt) {
				writeAffectationStatement(unit, (AffectationSt) st, sb, errors);
				
			} else {
				errors.add("Unhandled statement type: " + st.getClass().getSimpleName() + " " + st.getErr());
			}
			
			sb.append("\n");
			if(statementIndents.contains(st.getClass())) {
				indent++;
				indentHasStatement = false;
			} else {
				indentHasStatement = true;
			}
		}
	}
	
	private static List<Class<? extends Statement>> statementIndents = Arrays.asList(
			IfSt.class,
			ElseSt.class,
			ForSt.class
	);
	
	private static void writeIfStatement(Unit unit, IfSt s, StringBuilder sb, ErrorWrapper errors) {
		IfSt st = (IfSt) s;
		sb.append("if ");
		writeExpression(unit, st.getCondition(), sb, errors);
		sb.append(":");
	}
	
	private static void writeElseStatement(Unit unit, ElseSt st, StringBuilder sb, ErrorWrapper errors) {
		if(st.getCondition() == null) {
			sb.append("else:");
		} else {
			sb.append("elif ");
			writeExpression(unit, st.getCondition(), sb, errors);
			sb.append(":");
		}
	}
	
	private static void writeForStatement(Unit unit, ForSt st, StringBuilder sb, ErrorWrapper errors) {
		if(writeSimpleForStatement(unit, st, sb, errors)) {
			// simple for statement written
		} else {
//			writeVarDeclaration(unit, st.declaration, sb, errors);
//			sb.append("\nwhile true:");
			sb.append("for _ in range(0,0):");
			// FIX write while and affectation
		}
	}
	
	/**
	 * Simple for statements are statements that can be written as
	 * "<code>for x in range(a, b (,c)):</code>"<br>
	 * Note that {@code b} and {@code c} must be fixed (integer literals) for the
	 * for statement to be considered simple. Note again that
	 * <code>sizeof(array)</code> is not fixed! the {@code array} reference may be
	 * modified inside the for loop.
	 */
	private static boolean writeSimpleForStatement(Unit unit, ForSt st, StringBuilder sb, ErrorWrapper errors) {
		if(st.declaration == null || st.declaration.getType() != VarType.INT)
			return false;
		String simpleForVar = st.declaration.name;
		if(st.affectation == null || !(st.affectation.getVariable() instanceof VarExp))
			return false;
		if(!((VarExp) st.affectation.getVariable()).variable.equals(simpleForVar))
			return false;
		Expression affect = st.affectation.getValue();
		if(!(affect instanceof OperationExp))
			return false;
		OperationExp affectOp = (OperationExp) affect;
		if(affectOp.operator != Operator.ADD)
			return false;
		Expression alo = affectOp.getLeftOperand();
		Expression aro = affectOp.getRightOperand();
		if(!(aro instanceof IntLiteral) || !(alo instanceof VarExp) || !((VarExp) alo).variable.equals(simpleForVar))
			return false;
		if(st.condition == null || !(st.condition instanceof OperationExp))
			return false;
		OperationExp cond = (OperationExp) st.condition;
		Expression clo = cond.getLeftOperand();
		Expression cro = cond.getRightOperand();
		if(!(cro instanceof IntLiteral) || !(clo instanceof VarExp) || !((VarExp) clo).variable.equals(simpleForVar))
			return false;
		String max;
		if(cond.operator == Operator.LOWER)
			max = ((IntLiteral) cro).value.toString();
		else if(cond.operator == Operator.LEQUALS)
			max = ((IntLiteral) cro).value.toString() + "+1";
		else
			return false;
		sb.append("for " + simpleForVar + " in range(");
		writeExpression(unit, st.declaration.getDefaultValue(), sb, errors);
		sb.append(", " + max);
		if(((IntLiteral) aro).value != 1)
			sb.append(", " + ((IntLiteral) aro).value);
		sb.append("):");
		return true;
	}
	
	static void writeVarDeclaration(Unit unit, VariableDeclaration st, StringBuilder sb, ErrorWrapper errors) {
		if(st.getDefaultValue() != null) {
			sb.append(st.name + " = ");
			writeExpression(unit, st.getDefaultValue(), sb, errors);
		}
	}
	
	private static void writeReturnStatement(Unit unit, ReturnSt st, StringBuilder sb, ErrorWrapper errors) {
		sb.append("return");
		if(st.getExpression() != null) {
			sb.append(' ');
			writeExpression(unit, st.getExpression(), sb, errors);
		}
	}
	
	private static void writeAffectationStatement(Unit unit, AffectationSt st, StringBuilder sb, ErrorWrapper errors) {
		writeExpression(unit, st.getVariable(), sb, errors);
		sb.append(" = ");
		writeExpression(unit, st.getValue(), sb, errors);
	}
	
}
