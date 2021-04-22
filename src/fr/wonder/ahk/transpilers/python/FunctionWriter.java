package fr.wonder.ahk.transpilers.python;

import static fr.wonder.ahk.transpilers.python.ExpressionWriter.writeExpression;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.statements.AffectationSt;
import fr.wonder.ahk.compiled.statements.CompositeReturnSt;
import fr.wonder.ahk.compiled.statements.ElseSt;
import fr.wonder.ahk.compiled.statements.ForEachSt;
import fr.wonder.ahk.compiled.statements.ForSt;
import fr.wonder.ahk.compiled.statements.FunctionSt;
import fr.wonder.ahk.compiled.statements.IfSt;
import fr.wonder.ahk.compiled.statements.MultipleAffectationSt;
import fr.wonder.ahk.compiled.statements.RangedForSt;
import fr.wonder.ahk.compiled.statements.ReturnSt;
import fr.wonder.ahk.compiled.statements.SectionEndSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.statements.WhileSt;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.exceptions.ErrorWrapper;

class FunctionWriter {
	
	static void writeFunction(FunctionSection func, StringBuilder sb, ErrorWrapper errors) {
		sb.append("  @staticmethod\n");
		sb.append("  def " + func.getSignature().computedSignature + "(");
		sb.append(Utils.mapToString(func.arguments, arg->arg.name));
		sb.append("):\n");
		
		if(func.body.length == 0) {
			sb.append("    pass\n");
			return;
		}
		
		// write statements
		
		Set<String> globalVariables = new HashSet<>();
		
		for(Statement st : func.body) {
			for(Expression e : st.getExpressions())
				if(e instanceof VarExp && !((VarExp) e).declaration.isLocallyScoped())
					globalVariables.add(((VarExp) e).variable);		
		}
		
		if(!globalVariables.isEmpty()) {
			sb.append("    global ");
			for(String s : globalVariables)
				sb.append(s + ", ");
			sb.delete(sb.length()-2, sb.length());
			sb.append('\n');
		}
		
		int indent = 2;
		boolean indentHasStatement = true;
		LinkedList<AffectationSt> indentationEndAffectations = new LinkedList<>();
		
		for(int i = 0; i < func.body.length; i++) {
			Statement st = func.body[i];
			if(st instanceof SectionEndSt) {
				if(!indentationEndAffectations.isEmpty()) {
					sb.append("  ".repeat(indent));
					writeAffectationStatement(indentationEndAffectations.poll(), sb, errors);
					sb.append('\n');
				} else if(!indentHasStatement) {
					sb.append("  ".repeat(indent) + "pass\n");
				}
				indent--;
				indentHasStatement = true;
				continue;
			}
			
			// handle special statements
			if(st instanceof RangedForSt) {
				if(!isSimpleRangedForStatement((RangedForSt) st))
					st = ((RangedForSt) st).toComplexFor();
			}
			if(st instanceof ForSt) {
				if(((ForSt) st).declaration != null) {
					sb.append("  ".repeat(indent));
					writeVarDeclaration(((ForSt) st).declaration, sb, errors);
					sb.append('\n');
				}
				sb.append("  ".repeat(indent) + "while ");
				writeExpression(((ForSt) st).condition, sb, errors);
				sb.append(":\n");
				indentationEndAffectations.add(((ForSt) st).affectation);
				
			} else {
				// write common statements
				sb.append("  ".repeat(indent));
				writeStatement(st, sb, errors);
				sb.append('\n');
			}
			
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
			ForSt.class,
			RangedForSt.class,
			ForEachSt.class,
			WhileSt.class
	);
	
	private static void writeStatement(Statement st, StringBuilder sb, ErrorWrapper errors) {
		if(st instanceof VariableDeclaration) {
			writeVarDeclaration((VariableDeclaration) st, sb, errors);
			
		} else if(st instanceof IfSt) {
			writeIfStatement((IfSt) st, sb, errors);
			
		} else if(st instanceof ElseSt) {
			writeElseStatement((ElseSt) st, sb, errors);
			
		} else if(st instanceof RangedForSt) {
			writeRangedForStatement((RangedForSt) st, sb, errors);
			
		} else if(st instanceof ForEachSt) {
			writeForeachStatement((ForEachSt) st, sb, errors);
			
		} else if(st instanceof ReturnSt) {
			writeReturnStatement((ReturnSt) st, sb, errors);
			
		} else if(st instanceof CompositeReturnSt) {
			writeCompositeReturnStatement((CompositeReturnSt) st, sb, errors);
			
		} else if(st instanceof FunctionSt) {
			writeExpression(((FunctionSt) st).getFunction(), sb, errors);
			
		} else if(st instanceof AffectationSt) {
			writeAffectationStatement((AffectationSt) st, sb, errors);
			
		} else if(st instanceof MultipleAffectationSt) {
			writeMultipleAffectationStatement((MultipleAffectationSt) st, sb, errors);
			
		} else if(st instanceof WhileSt) {
			writeWhileStatement((WhileSt) st, sb, errors);
			
		} else {
			errors.add("Unhandled statement type: " + st.getClass().getSimpleName() + " " + st.getErr());
		}
	}
	
	private static void writeWhileStatement(WhileSt st, StringBuilder sb, ErrorWrapper errors) {
		sb.append("while ");
		writeExpression(st.getCondition(), sb, errors);
		sb.append(':');
	}

	private static void writeIfStatement(IfSt s, StringBuilder sb, ErrorWrapper errors) {
		IfSt st = (IfSt) s;
		sb.append("if ");
		writeExpression(st.getCondition(), sb, errors);
		sb.append(":");
	}
	
	private static void writeElseStatement(ElseSt st, StringBuilder sb, ErrorWrapper errors) {
		if(st.getCondition() == null) {
			sb.append("else:");
		} else {
			sb.append("elif ");
			writeExpression(st.getCondition(), sb, errors);
			sb.append(":");
		}
	}
	
	private static boolean isSimpleRangedForStatement(RangedForSt st) {
		return st.getStep() instanceof IntLiteral && st.getMax() instanceof IntLiteral;
	}
	
	private static void writeRangedForStatement(RangedForSt st, StringBuilder sb, ErrorWrapper errors) {
		sb.append("for " + st.variable + " in range(");
		writeExpression(st.getMin(), sb, errors);
		sb.append(", " + ((IntLiteral) st.getMax()).value);
		if(((IntLiteral) st.getStep()).value != 1)
			sb.append(", " + ((IntLiteral) st.getStep()).value);
		sb.append("):");
	}
	
	private static void writeForeachStatement(ForEachSt st, StringBuilder sb, ErrorWrapper errors) {
		sb.append("for " + st.declaration.name + " in ");
		writeExpression(st.getIterable(), sb, errors);
		sb.append(":");
	}
	
	static void writeVarDeclaration(VariableDeclaration st, StringBuilder sb, ErrorWrapper errors) {
		if(st.getDefaultValue() != null) {
			sb.append(st.name + " = ");
			writeExpression(st.getDefaultValue(), sb, errors);
		}
	}
	
	private static void writeReturnStatement(ReturnSt st, StringBuilder sb, ErrorWrapper errors) {
		sb.append("return");
		if(st.getExpression() != null) {
			sb.append(' ');
			writeExpression(st.getExpression(), sb, errors);
		}
	}

	private static void writeCompositeReturnStatement(CompositeReturnSt st, StringBuilder sb, ErrorWrapper errors) {
		sb.append("return ");
		Expression[] returnValues = st.getExpressions();
		for(int i = 0; i < returnValues.length; i++) {
			writeExpression(returnValues[i], sb, errors);
			if(i != returnValues.length-1)
				sb.append(", ");
		}
	}
	
	private static void writeAffectationStatement(AffectationSt st, StringBuilder sb, ErrorWrapper errors) {
		writeExpression(st.getVariable(), sb, errors);
		sb.append(" = ");
		writeExpression(st.getValue(), sb, errors);
	}
	
	private static void writeMultipleAffectationStatement(MultipleAffectationSt st, StringBuilder sb,
			ErrorWrapper errors) {
		String[] variables = st.getVariablesNames();
		for(int i = 0; i < variables.length; i++) {
			sb.append(variables[i]);
			if(i != variables.length-1)
				sb.append(",");
		}
		sb.append(" = ");
		Expression[] values = st.getValues();
		for(int i = 0; i < values.length; i++) {
			writeExpression(values[i], sb, errors);
			if(i != values.length-1)
				sb.append(",");
		}
	}
	
}
