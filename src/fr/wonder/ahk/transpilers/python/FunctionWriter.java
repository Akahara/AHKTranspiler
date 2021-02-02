package fr.wonder.ahk.transpilers.python;

import static fr.wonder.ahk.transpilers.python.ExpressionWriter.writeExpression;

import java.util.Arrays;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.OperationExp;
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
		// TODO write complex for statements
		// for now only for statements such as:
		// for(int <var> : <exp>..<exp>(..<exp>)) {
		// can be compiled
		sb.append("for " + st.declaration.name + " in range(");
		writeExpression(unit, st.declaration.getDefaultValue(), sb, errors);
		sb.append(", ");
		writeExpression(unit, ((OperationExp)st.condition).getRightOperand(), sb, errors);
		if(((IntLiteral)((OperationExp)st.affectation.getValue()).getRightOperand()).value != 1) {
			sb.append(", ");
			writeExpression(unit, ((OperationExp)st.affectation.getValue()).getRightOperand(), sb, errors);
		}
		sb.append("):");
		
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
