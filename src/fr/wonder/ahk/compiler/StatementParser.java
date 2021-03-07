package fr.wonder.ahk.compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.DirectAccessExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.FunctionCallExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.AffectationSt;
import fr.wonder.ahk.compiled.statements.ElseSt;
import fr.wonder.ahk.compiled.statements.ForSt;
import fr.wonder.ahk.compiled.statements.FunctionSt;
import fr.wonder.ahk.compiled.statements.IfSt;
import fr.wonder.ahk.compiled.statements.LabeledStatement;
import fr.wonder.ahk.compiled.statements.ReturnSt;
import fr.wonder.ahk.compiled.statements.SectionEndSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.statements.WhileSt;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class StatementParser {

	public static Statement parseStatement(UnitSource source, Token[] line, ErrorWrapper errors) {
		
		TokenBase firstToken = line[0].base;
		switch(firstToken) {
		case KW_IF:
			return parseIfStatement(source, line, errors);
		case KW_ELSE:
			return parseElseStatement(source, line, errors);
		case KW_WHILE:
			return parseWhileStatement(source, line, errors);
		case KW_FOR:
			return parseForStatement(source, line, errors);
		case KW_RETURN:
			return parseReturnStatement(source, line, errors);
		case TK_BRACE_CLOSE:
			return new SectionEndSt(source, line[0].sourceStart, line[line.length-1].sourceStop);
		case TYPE_BOOL:
		case TYPE_FLOAT:
		case TYPE_INT:
		case TYPE_STR:
		case VAR_UNIT:
			return parseVariableDeclaration(source, line, errors);
		default:
			break;
		}
		TokenBase lastToken = line[line.length-1].base;
		if(Tokens.isDirectAffectation(lastToken))
			return parseDirectAffectationStatement(source, line, errors);
		if(isAffectationStatement(line))
			return parseAffectationStatement(source, line, errors);
		if(isFunctionStatement(line))
			return parseFunctionStatement(source, line, errors);
		
		errors.add("Unknown statement:" + source.getErr(line));
		return null;
	}

	private static AffectationSt parseDirectAffectationStatement(UnitSource source, Token[] line, ErrorWrapper errors) {
		ErrorWrapper subErrors = errors.subErrrors("Unable to parse variable affectation");
		Expression leftOperand = ExpressionParser.parseExpression(source, line, 0, line.length-1, subErrors);
		Token last = line[line.length-1];
		Expression rightOperand = new IntLiteral(source, last.sourceStart, last.sourceStop, 1);
		Operator op = Tokens.getDirectOperation(last.base);
		Expression affectation = new OperationExp(source, line[0].sourceStart, last.sourceStop, op, leftOperand, rightOperand);
		return new AffectationSt(source, line[0].sourceStart, last.sourceStop, leftOperand, affectation);
	}

	// TODO parse var declaration modifiers (visibility, constants...)
	/** Assumes that the first line token base is a var type */
	public static VariableDeclaration parseVariableDeclaration(UnitSource source, Token[] line, ErrorWrapper errors) {
		ErrorWrapper subErrors = errors.subErrrors("Unable to parse variable declaration");
		
		if(line.length < 2) {
			subErrors.add("Invalid variable declaration:" + line[0].getErr());
			return null;
		}
		
		int arrayDimensions = 0;
		int t = 1;
		while(t < line.length-2 && line[t].base == TokenBase.TK_BRACKET_OPEN) {
			t++;
			arrayDimensions++;
			if(line[t].base == TokenBase.TK_BRACKET_CLOSE)
				t++;
		}
		
		if(line[t].base != TokenBase.VAR_VARIABLE) {
			subErrors.add("Expected variable name:" + line[t].getErr());
			return null;
		}
		
		if(line.length > t+1 && line[t+1].base != TokenBase.KW_EQUAL) {
			subErrors.add("Expected affectation value:" + line[t+1].getErr());
			return null;
		}
		
		String varName = line[t].text;
		VarType varType = Tokens.getType(line[0]);
		
		if(varType == null)
			subErrors.add("Unknown value type:" + line[0].getErr());
		
		for(int i = 0; i < arrayDimensions; i++)
			varType = new VarArrayType(varType);
		
		if(line.length == t+1) {
			return new VariableDeclaration(source, line[0].sourceStart, line[line.length-1].sourceStop, varName, varType);
		} else {
			Expression defaultValue = ExpressionParser.parseExpression(source, line, t+2, line.length, subErrors);
			return new VariableDeclaration(source, line[0].sourceStart, line[line.length-1].sourceStop, varName, varType, defaultValue);
		}
	}
	
	/** Assumes that the first token is KW_IF */
	private static Statement parseIfStatement(UnitSource source, Token[] line, ErrorWrapper errors) {
		if(line.length < 3) {
			errors.add("Incomplete if statement:" + source.getErr(line));
			return null;
		}
		boolean err = false;
		if(line[1].base != TokenBase.TK_PARENTHESIS_OPEN) {
			errors.add("Expected '('" + line[1].getErr());
			err = true;
		}
		boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
		int conditionEnd = line.length-1-(singleLine ? 0 : 1);
		if(line[conditionEnd].base != TokenBase.TK_PARENTHESIS_CLOSE) {
			errors.add("Expected ')'" + line[conditionEnd].getErr());
			err = true;
		}
		if(err) {
			return null;
		} else {
			Expression condition = ExpressionParser.parseExpression(source, line, 2, conditionEnd, errors);
			return new IfSt(source, line[0].sourceStart, line[line.length-1].sourceStop, condition, singleLine);
		}
	}
	
	/** Assumes that the first token is KW_ELSE */
	private static Statement parseElseStatement(UnitSource source, Token[] line, ErrorWrapper errors) {
		boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
		return new ElseSt(source, line[0].sourceStart, line[line.length-1].sourceStop, singleLine);
	}
	
	/** Assumes that the first token is KW_WHILE */
	private static Statement parseWhileStatement(UnitSource source, Token[] line, ErrorWrapper errors) {
		boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
		if(line.length < 3) {
			errors.add("Incomplete while statement:" + source.getErr(line));
			return null;
		}
		boolean err = false;
		if(line[1].base != TokenBase.TK_PARENTHESIS_OPEN) {
			errors.add("Expected '('" + line[1].getErr());
			err = true;
		}
		int conditionEnd = line.length-1-(singleLine ? 0 : 1);
		if(line[conditionEnd].base != TokenBase.TK_PARENTHESIS_CLOSE) {
			errors.add("Expected ')'" + line[conditionEnd].getErr());
			err = true;
		}
		if(err) {
			return null;
		} else {
			Expression condition = ExpressionParser.parseExpression(source, line, 2, conditionEnd, errors);
			return new WhileSt(source, line[0].sourceStart, line[line.length-1].sourceStop, condition, singleLine);
		}
	}
	
	/** Assumes that the first token is KW_FOR */
	private static Statement parseForStatement(UnitSource source, Token[] line, ErrorWrapper errors) {
		boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
		if(line.length < 3) {
			errors.add("Incomplete for statement:" + source.getErr(line));
			return null;
		}
		boolean err = false;
		if(line[1].base != TokenBase.TK_PARENTHESIS_OPEN) {
			errors.add("Expected '('" + line[1].getErr());
			err = true;
		}
		int conditionEnd = line.length-1-(singleLine ? 0 : 1);
		if(line[conditionEnd].base != TokenBase.TK_PARENTHESIS_CLOSE) {
			errors.add("Expected ')'" + line[conditionEnd].getErr());
			err = true;
		}
		if(err)
			return null;
		
		int simpleRangeMarker = Utils.getTokenIdx(line, TokenBase.TK_DOUBLE_DOT, 2);
		VariableDeclaration declaration = null;
		Expression condition = null;
		AffectationSt affectation = null;
		
		if(simpleRangeMarker != -1) {
			int simpleRangeSecond = Utils.getTokenIdx(line, TokenBase.TK_DOUBLE_DOT, simpleRangeMarker+1);
			int equalsMarker = Utils.getTokenIdx(line, TokenBase.KW_EQUAL, 2);
			if(equalsMarker == -1)
				equalsMarker = Utils.getTokenIdx(line, TokenBase.TK_COLUMN, 2);
			// parse simple range for
			if(equalsMarker == -1) {
				errors.add("Invalid for-in-range declaration:" + source.getErr(line));
				return null;
			}
			// replace ':' by '=' (necessary to parse declaration)
			line[equalsMarker] = new Token(source, TokenBase.KW_EQUAL, "=", line[equalsMarker].sourceStart);
			Expression rangeTarget = null;
			if(Tokens.isVarType(line[2].base)) {
				declaration = parseVariableDeclaration(source, Arrays.copyOfRange(line, 2, simpleRangeMarker), errors);
				if(declaration != null)
					rangeTarget = new VarExp(source, declaration.sourceStart, declaration.sourceStop, declaration.getName());
				else
					errors.add("Missing range target in for statement:" + source.getErr(line, 2, simpleRangeMarker));
			} else {
				rangeTarget = ExpressionParser.parseExpression(source, line, 2, equalsMarker, errors);
				if(!(rangeTarget instanceof VarExp) && !(rangeTarget instanceof DirectAccessExp))
					errors.add("Invalid for-in-range over a non variable expression:" + source.getErr(line, 2, equalsMarker));
			}
			int maxStop = simpleRangeSecond == -1 ? conditionEnd : simpleRangeSecond;
			Expression maximum = ExpressionParser.parseExpression(source, line, simpleRangeMarker+1, maxStop, errors);
			Expression increment;
			if(simpleRangeSecond != -1)
				increment = ExpressionParser.parseExpression(source, line, simpleRangeSecond+1, conditionEnd, errors);
			else
				increment = new LiteralExp.IntLiteral(source, line[conditionEnd].sourceStart, line[conditionEnd].sourceStart, 1);
			int incSourceStart = line[2].sourceStart;
			int incSourceStop = line[conditionEnd-1].sourceStop;
			OperationExp affectationValue = new OperationExp(source, incSourceStart, incSourceStop, Operator.ADD, rangeTarget, increment);
			affectation = new AffectationSt(source, incSourceStart, incSourceStop, rangeTarget, affectationValue);
			condition = new OperationExp(source, incSourceStart, incSourceStop, Operator.LOWER, rangeTarget, maximum);
			// FIX if possible convert complex to simple for
		} else {
			// parse complex for
			int firstSplit = Utils.getTokenIdx(line, TokenBase.TK_COLUMN, 2);
			int secondSplit = Utils.getTokenIdx(line, TokenBase.TK_COLUMN, firstSplit+1);
			if(firstSplit == -1 || secondSplit == -1) {
				errors.add("Invalid for statement:" + source.getErr(line));
				return null;
			}
			
			if(firstSplit != 2 && !Tokens.isVarType(line[2].base)) {
				errors.add("Expected variable declaration in for statement:" + source.getErr(line, 2, firstSplit));
				return null;
			}
			
			if(firstSplit != 2)
				declaration = parseVariableDeclaration(source, Arrays.copyOfRange(line, 2, firstSplit),
						errors.subErrrors("Expected variable declaration in for statement"));
			if(secondSplit != firstSplit+1)
				condition = ExpressionParser.parseExpression(source, line, firstSplit+1, secondSplit, 
						errors.subErrrors("Expected condition in for statement"));
			if(conditionEnd != secondSplit+1) {
				Token[] affectationTokens = Arrays.copyOfRange(line, secondSplit+1, conditionEnd);
				ErrorWrapper subErrors = errors.subErrrors("Expected affectation in for statement");
				if(Tokens.isDirectAffectation(affectationTokens[affectationTokens.length-1].base))
					affectation = parseDirectAffectationStatement(source, affectationTokens, subErrors);
				else
					affectation = parseAffectationStatement(source, affectationTokens, subErrors);
			}
			
		}
		
		return new ForSt(source, line[0].sourceStart, line[line.length-1].sourceStop,
				singleLine, declaration, condition, affectation);
	}
	
	/** Assumes that the first token is KW_RETURN */
	private static Statement parseReturnStatement(UnitSource unit, Token[] line, ErrorWrapper errors) {
		if(line.length == 1) {
			return new ReturnSt(unit, line[0].sourceStart, line[line.length-1].sourceStop);
		} else {
			Expression returnValue = ExpressionParser.parseExpression(unit, line, 1, line.length, errors);
			return new ReturnSt(unit, line[0].sourceStart, line[line.length-1].sourceStop, returnValue);
		}
	}
	
	private static boolean isAffectationStatement(Token[] line) {
		for(Token t : line) {
			if(Tokens.isAffectationOperator(t.base))
				return true;
		}
		return false;
	}
	
	private static AffectationSt parseAffectationStatement(UnitSource source, Token[] line, ErrorWrapper errors) {
		int opPos = -1;
		for(int i = 0; i < line.length; i++) {
			if(Tokens.isAffectationOperator(line[i].base)) {
				opPos = i;
				break;
			}
		}
		if(opPos == -1)
			throw new IllegalAccessError("Not an affectation " + source.getErr(line));
		
		Expression leftOperand = ExpressionParser.parseExpression(source, line, 0, opPos, errors);
		Expression rightOperand = ExpressionParser.parseExpression(source, line, opPos+1, line.length, errors);
		if(line[opPos].base != TokenBase.KW_EQUAL) {
			Operator op = Tokens.getAffectationOperator(line[opPos].base);
			rightOperand = new OperationExp(source, line[0].sourceStart, line[line.length-1].sourceStop,
					op, leftOperand, rightOperand);
		}
		return new AffectationSt(source, line[0].sourceStart, line[line.length-1].sourceStop, leftOperand, rightOperand);
	}
	
	private static boolean isFunctionStatement(Token[] line) {
		return line[line.length-1].base == TokenBase.TK_PARENTHESIS_CLOSE;
	}
	
	private static Statement parseFunctionStatement(UnitSource source, Token[] line, ErrorWrapper errors) {
		Expression exp = ExpressionParser.parseExpression(source, line, 0, line.length, errors);
		if(exp instanceof FunctionCallExp)
			return new FunctionSt(source, line[0].sourceStart, line[line.length-1].sourceStop, (FunctionCallExp) exp);
		else
			throw new IllegalStateException("Statement is not a function " + source.getErr(line));
	}

	/** Adds functionEndSt to complete single line ifs, elses ... */
	public static void finalizeStatements(UnitSource source, FunctionSection function) {
		List<Statement> statements = new ArrayList<>(Arrays.asList(function.body));
		
		// close single line statements
		for(int s = 0; s < statements.size(); s++) {
			Statement st = statements.get(s);
			if(st instanceof LabeledStatement) {
				s = closeStatement(source, statements, s)-1;
			}
		}
		
		function.body = statements.toArray(Statement[]::new);
	}
	
	private static Map<Class<? extends LabeledStatement>, Class<? extends LabeledStatement>> sectionsPairs = Map.of(
			IfSt.class, ElseSt.class
	);
	
	private static int closeStatement(UnitSource source, List<Statement> statements, int idx) {
		LabeledStatement toClose = (LabeledStatement) statements.get(idx);
		if(toClose.singleLine) {
			if(statements.size() == idx) {
				statements.add(new SectionEndSt(source, statements.get(statements.size()-1).sourceStop));
				return statements.size();
			} else if(statements.get(idx+1) instanceof LabeledStatement) {
				idx = closeStatement(source, statements, idx+1);
			} else {
				idx += 2;
			}
			statements.add(idx, new SectionEndSt(source, statements.get(idx-1).sourceStop));
			idx++;
			// handle section-end special cases
			if(statements.size() != idx && sectionsPairs.get(toClose.getClass()) == statements.get(idx).getClass())
				idx = closeStatement(source, statements, idx);
			return idx;
		} else {
			for(int s = idx+1; s < statements.size(); s++) {
				Statement st = statements.get(s);
				if(st instanceof LabeledStatement) {
					s = closeStatement(source, statements, s);
				} else if(st instanceof SectionEndSt) {
					s++;
					// handle section-end special cases
					if(statements.size() != s && sectionsPairs.get(toClose.getClass()) == statements.get(idx).getClass())
						s = closeStatement(source, statements, s);
					return s;
				}
			}
			return statements.size();
		}
	}
	
}
