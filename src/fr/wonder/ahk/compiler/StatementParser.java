package fr.wonder.ahk.compiler;

import java.util.Arrays;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.FunctionCallExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.BoolLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.FloatLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.NullExp;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.AffectationSt;
import fr.wonder.ahk.compiled.statements.ElseSt;
import fr.wonder.ahk.compiled.statements.ForEachSt;
import fr.wonder.ahk.compiled.statements.ForSt;
import fr.wonder.ahk.compiled.statements.FunctionSt;
import fr.wonder.ahk.compiled.statements.IfSt;
import fr.wonder.ahk.compiled.statements.RangedForSt;
import fr.wonder.ahk.compiled.statements.ReturnSt;
import fr.wonder.ahk.compiled.statements.SectionEndSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.statements.WhileSt;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnreachableException;

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
		case KW_FOREACH:
			return parseForeachStatement(source, line, errors);
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
		return Invalids.STATEMENT;
	}
	
	
	private static boolean assertParentheses(Token[] line, int begin, int end, ErrorWrapper errors) {
		boolean success = true;
		if(begin != -1 && line[begin].base != TokenBase.TK_PARENTHESIS_OPEN) {
			errors.add("Expected ')' :" + line[begin].getErr());
			success = false;
		}
		if(end != -1 && line[end].base != TokenBase.TK_PARENTHESIS_CLOSE) {
			errors.add("Expected ')' :" + line[end].getErr());
			success = false;
		}
		return success;
	}

	private static AffectationSt parseDirectAffectationStatement(UnitSource source, Token[] line, ErrorWrapper errors) {
		ErrorWrapper subErrors = errors.subErrrors("Unable to parse variable affectation");
		Expression leftOperand = ExpressionParser.parseExpression(source, line, 0, line.length-1, subErrors);
		if(!subErrors.noErrors())
			return Invalids.AFFECTATION_STATEMENT;
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
			return Invalids.VARIABLE_DECLARATION;
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
			return Invalids.VARIABLE_DECLARATION;
		}
		
		if(line.length > t+1 && (line[t+1].base != TokenBase.KW_EQUAL || line.length == t+2)) {
			subErrors.add("Expected affectation value:" + line[t+1].getErr());
			return Invalids.VARIABLE_DECLARATION;
		}
		
		String varName = line[t].text;
		VarType varType = Tokens.getType(line[0]);
		
		if(varType == null) {
			subErrors.add("Unknown value type:" + line[0].getErr());
			varType = Invalids.TYPE;
		}
		
		for(int i = 0; i < arrayDimensions; i++)
			varType = new VarArrayType(varType);
		
		Expression defaultValue;
		
		if(line.length == t+1) {
			// get default value
			int sourceLoc = line[t].sourceStop;
			if(varType == VarType.INT)
				defaultValue = new IntLiteral(source, sourceLoc, sourceLoc, 0);
			else if(varType == VarType.FLOAT)
				defaultValue = new FloatLiteral(source, sourceLoc, sourceLoc, 0);
			else if(varType == VarType.BOOL)
				defaultValue = new BoolLiteral(source, sourceLoc, sourceLoc, false);
			else if(varType == VarType.STR || varType instanceof VarStructType)
				defaultValue = new NullExp(source, sourceLoc, sourceLoc);
			else if(varType == Invalids.TYPE)
				defaultValue = Invalids.EXPRESSION;
			else
				throw new UnreachableException("Unimplemented type default value");
		} else {
			defaultValue = ExpressionParser.parseExpression(source, line, t+2, line.length, subErrors);
		}
		return new VariableDeclaration(source, line[0].sourceStart, line[line.length-1].sourceStop, varName, varType, defaultValue);
	}
	
	/** Assumes that the first token is KW_IF */
	private static Statement parseIfStatement(UnitSource source, Token[] line, ErrorWrapper errors) {
		if(line.length < 3) {
			errors.add("Incomplete if statement:" + source.getErr(line));
			return Invalids.STATEMENT;
		}
		boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
		int conditionEnd = line.length-1-(singleLine ? 0 : 1);
		if(!assertParentheses(line, 1, conditionEnd, errors)) {
			return Invalids.STATEMENT;
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
		if(line.length < 3) {
			errors.add("Incomplete while statement:" + source.getErr(line));
			return Invalids.STATEMENT;
		}
		boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
		int conditionEnd = line.length-1-(singleLine ? 0 : 1);
		if(!assertParentheses(line, 1, conditionEnd, errors)) {
			return Invalids.STATEMENT;
		} else {
			Expression condition = ExpressionParser.parseExpression(source, line, 2, conditionEnd, errors);
			return new WhileSt(source, line[0].sourceStart, line[line.length-1].sourceStop, condition, singleLine);
		}
	}
	
	/** Assumes that the first token is KW_FOR */
	private static Statement parseForStatement(UnitSource source, Token[] line, ErrorWrapper errors) {
		if(line.length < 3) {
			errors.add("Incomplete for statement:" + source.getErr(line));
			return Invalids.STATEMENT;
		}
		boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
		int conditionEnd = line.length-1-(singleLine ? 0 : 1);
		if(!assertParentheses(line, 1, conditionEnd, errors))
			return Invalids.STATEMENT;
		
		int simpleRangeMarker = Utils.getTokenIdx(line, TokenBase.TK_DOUBLE_DOT, 2);
		
		if(simpleRangeMarker != -1)
			return parseRangedFor(source, line, conditionEnd, simpleRangeMarker, singleLine, errors);
		else
			return parseComplexFor(source, line, conditionEnd, singleLine, errors);
	}

	private static Statement parseComplexFor(UnitSource source, Token[] line, int conditionEnd, boolean singleLine,
			ErrorWrapper errors) {
		VariableDeclaration declaration = null;
		Expression condition;
		AffectationSt affectation = null;
		// parse complex for
		int firstSplit = Utils.getTokenIdx(line, TokenBase.TK_COLUMN, 2);
		int secondSplit = Utils.getTokenIdx(line, TokenBase.TK_COLUMN, firstSplit+1);
		if(firstSplit == -1 || secondSplit == -1) {
			errors.add("Invalid for statement:" + source.getErr(line));
			return Invalids.STATEMENT;
		}
		if(firstSplit != 2 && !Tokens.isVarType(line[2].base)) {
			errors.add("Expected variable declaration in for statement:" + source.getErr(line, 2, firstSplit));
			return Invalids.STATEMENT;
		}
		if(firstSplit != 2) {
			declaration = parseVariableDeclaration(source, Arrays.copyOfRange(line, 2, firstSplit),
					errors.subErrrors("Expected variable declaration in for statement"));
		}
		if(secondSplit != firstSplit+1) {
			condition = ExpressionParser.parseExpression(source, line, firstSplit+1, secondSplit, 
					errors.subErrrors("Expected condition in for statement"));
		} else {
			condition = new BoolLiteral(source, line[firstSplit].sourceStop, line[secondSplit].sourceStart, true);
		}
		if(conditionEnd != secondSplit+1) {
			Token[] affectationTokens = Arrays.copyOfRange(line, secondSplit+1, conditionEnd);
			ErrorWrapper subErrors = errors.subErrrors("Expected affectation in for statement");
			if(Tokens.isDirectAffectation(affectationTokens[affectationTokens.length-1].base))
				affectation = parseDirectAffectationStatement(source, affectationTokens, subErrors);
			else
				affectation = parseAffectationStatement(source, affectationTokens, subErrors);
		}
		// TODO if possible, convert to ranged for
		return new ForSt(source, line[0].sourceStart, line[line.length-1].sourceStop,
				singleLine, declaration, condition, affectation);
	}

	private static Statement parseRangedFor(UnitSource source, Token[] line, int conditionEnd,
			int simpleRangeMarker, boolean singleLine, ErrorWrapper errors) {
		int simpleRangeSecond = Utils.getTokenIdx(line, TokenBase.TK_DOUBLE_DOT, simpleRangeMarker+1);
		int equalsMarker = Utils.getTokenIdx(line, TokenBase.KW_EQUAL, 2);
		if(equalsMarker == -1)
			equalsMarker = Utils.getTokenIdx(line, TokenBase.TK_COLUMN, 2);
		// parse simple range for
		if(equalsMarker == -1) {
			errors.add("Invalid for-in-range declaration:" + source.getErr(line));
			return Invalids.STATEMENT;
		}
		// replace ':' by '=' (necessary to parse declaration)
		line[equalsMarker] = new Token(source, TokenBase.KW_EQUAL, "=", line[equalsMarker].sourceStart);
		if(line[2].base != TokenBase.TYPE_INT) {
			errors.add("Missing range target in for statement:" + source.getErr(line, 2, simpleRangeMarker));
			return Invalids.STATEMENT;
		}
		VariableDeclaration declaration = parseVariableDeclaration(source, Arrays.copyOfRange(line, 2, simpleRangeMarker), errors);
		int maxStop = simpleRangeSecond == -1 ? conditionEnd : simpleRangeSecond;
		Expression maximum = ExpressionParser.parseExpression(source, line, simpleRangeMarker+1, maxStop, errors);
		Expression increment;
		if(simpleRangeSecond != -1)
			increment = ExpressionParser.parseExpression(source, line, simpleRangeSecond+1, conditionEnd, errors);
		else
			increment = new IntLiteral(source, line[conditionEnd].sourceStart, line[conditionEnd].sourceStart, 1);
		return new RangedForSt(source, line[0].sourceStart, line[line.length-1].sourceStop,
				singleLine, declaration.name, declaration.getDefaultValue(), maximum, increment);
	}

	/** Assumes that the first token is KW_FOREACH */
	private static Statement parseForeachStatement(UnitSource source, Token[] line, ErrorWrapper errors) {
		if(line.length < 5) {
			errors.add("Incomplete foreach statement" + source.getErr(line));
			return Invalids.STATEMENT;
		}
		boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
		int conditionEnd = line.length-1-(singleLine ? 0 : 1);
		if(!assertParentheses(line, 1, conditionEnd, errors))
			return Invalids.STATEMENT;
		if(!Tokens.isVarType(line[2].base) || line[3].base != TokenBase.VAR_VARIABLE) {
			errors.add("Expected variable declaration " + source.getErr(line, 2, 3));
			return Invalids.STATEMENT;
		}
		if(line[4].base != TokenBase.TK_COLUMN) {
			errors.add("Expected ':' at" + line[4].getErr());
			return Invalids.STATEMENT;
		}
		VariableDeclaration var = new VariableDeclaration(source, line[2].sourceStart, line[3].sourceStop,
				line[3].text, Tokens.getType(line[2]), null);
		Expression iterable = ExpressionParser.parseExpression(source, line, 5, conditionEnd, errors);
		
		return new ForEachSt(source, line[0].sourceStart, line[line.length-1].sourceStop, singleLine, var, iterable);
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
	
}
