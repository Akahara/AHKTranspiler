package fr.wonder.ahk.compiler.parser;

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
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.parser.ExpressionParser.Section;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnreachableException;

public class StatementParser extends AbstractParser {

	public static Statement parseStatement(Unit unit, Token[] line, ErrorWrapper errors) {
		
		TokenBase firstToken = line[0].base;
		switch(firstToken) {
		case KW_IF:
			return parseIfStatement(unit, line, errors);
		case KW_ELSE:
			return parseElseStatement(unit, line, errors);
		case KW_WHILE:
			return parseWhileStatement(unit, line, errors);
		case KW_FOR:
			return parseForStatement(unit, line, errors);
		case KW_FOREACH:
			return parseForeachStatement(unit, line, errors);
		case KW_RETURN:
			return parseReturnStatement(unit, line, errors);
		case TK_BRACE_CLOSE:
			return new SectionEndSt(unit.source, line[0].sourceStart, line[line.length-1].sourceStop);
		case TYPE_BOOL:
		case TYPE_FLOAT:
		case TYPE_INT:
		case TYPE_STR:
		case VAR_UNIT:
			return parseVariableDeclaration(unit, line, errors);
		default:
			break;
		}
		TokenBase lastToken = line[line.length-1].base;
		int affectationTokenPos = getAffectationTokenPos(line);
		if(Tokens.isDirectAffectation(lastToken))
			return parseDirectAffectationStatement(unit, line, errors);
		if(affectationTokenPos != -1 && isMultipleAffectationStatement(line, affectationTokenPos))
			return parseMultipleAffectationStatement(unit, line, affectationTokenPos, errors);
		if(affectationTokenPos != -1)
			return parseAffectationStatement(unit, line, affectationTokenPos, errors);
		if(canBeFunctionStatement(line))
			return parseFunctionStatement(unit, line, errors);
		
		errors.add("Unknown statement:" + unit.source.getErr(line));
		return Invalids.STATEMENT;
	}
	
	
	private static AffectationSt parseDirectAffectationStatement(Unit unit, Token[] line, ErrorWrapper errors) {
		ErrorWrapper subErrors = errors.subErrrors("Unable to parse variable affectation");
		Expression leftOperand = ExpressionParser.parseExpression(unit, line, 0, line.length-1, subErrors);
		if(!subErrors.noErrors())
			return Invalids.AFFECTATION_STATEMENT;
		Token last = line[line.length-1];
		Expression rightOperand = new IntLiteral(unit.source, last.sourceStart, last.sourceStop, 1);
		Operator op = Tokens.getDirectOperation(last.base);
		Expression affectation = new OperationExp(unit.source, line[0].sourceStart, last.sourceStop, op, leftOperand, rightOperand);
		return new AffectationSt(unit.source, line[0].sourceStart, last.sourceStop, leftOperand, affectation);
	}

	// TODO parse var declaration modifiers (visibility, constants...)
	public static VariableDeclaration parseVariableDeclaration(Unit unit, Token[] line, ErrorWrapper errors) {
		try {
			Pointer pointer = new Pointer();
			
			assertHasNext(line, pointer, "Invalid variable declaration", errors);
			
			VarType type = parseType(unit, line, pointer, errors);
			
			expectToken(line[pointer.position], TokenBase.VAR_VARIABLE, "Expected variable name", errors);
			String varName = line[pointer.position++].text;
			
			Expression defaultValue;
			if(line.length == pointer.position) {
				defaultValue = getDefaultValue(type, unit.source, line[line.length-1].sourceStop);
			} else {
				assertHasNext(line, pointer, "Expected affectation value", errors, 2);
				expectToken(line[pointer.position++], TokenBase.KW_EQUAL, "Expected affectation value", errors);
				defaultValue = ExpressionParser.parseExpression(unit, line, pointer.position, line.length, errors);
			}
			
			return new VariableDeclaration(unit, line[0].sourceStart, line[line.length-1].sourceStop, varName, type, defaultValue);
		} catch (ParsingException e) {
			return Invalids.VARIABLE_DECLARATION;
		}
	}
	
	public static Expression getDefaultValue(VarType type, UnitSource source, int sourceLoc) {
		if(type == VarType.INT)
			return new IntLiteral(source, sourceLoc, sourceLoc, 0);
		else if(type == VarType.FLOAT)
			return new FloatLiteral(source, sourceLoc, sourceLoc, 0);
		else if(type == VarType.BOOL)
			return new BoolLiteral(source, sourceLoc, sourceLoc, false);
		else if(type == VarType.STR || type instanceof VarStructType)
			return new NullExp(source, sourceLoc, sourceLoc);
		else if(type == Invalids.TYPE)
			return Invalids.EXPRESSION;
		else if(type instanceof VarArrayType)
			return new NullExp(source, sourceLoc, sourceLoc);
		else
			throw new UnreachableException("Unimplemented type default value for " + type);
	}
	
	/** Assumes that the first token is KW_IF */
	private static Statement parseIfStatement(Unit unit, Token[] line, ErrorWrapper errors) {
		if(line.length < 3) {
			errors.add("Incomplete if statement:" + unit.source.getErr(line));
			return Invalids.STATEMENT;
		}
		boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
		int conditionEnd = line.length-1-(singleLine ? 0 : 1);
		if(!assertParentheses(line, 1, conditionEnd, errors)) {
			return Invalids.STATEMENT;
		} else {
			Expression condition = ExpressionParser.parseExpression(unit, line, 2, conditionEnd, errors);
			return new IfSt(unit.source, line[0].sourceStart, line[line.length-1].sourceStop, condition, singleLine);
		}
	}
	
	/** Assumes that the first token is KW_ELSE */
	private static Statement parseElseStatement(Unit unit, Token[] line, ErrorWrapper errors) {
		boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
		return new ElseSt(unit.source, line[0].sourceStart, line[line.length-1].sourceStop, singleLine);
	}
	
	/** Assumes that the first token is KW_WHILE */
	private static Statement parseWhileStatement(Unit unit, Token[] line, ErrorWrapper errors) {
		if(line.length < 3) {
			errors.add("Incomplete while statement:" + unit.source.getErr(line));
			return Invalids.STATEMENT;
		}
		boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
		int conditionEnd = line.length-1-(singleLine ? 0 : 1);
		if(!assertParentheses(line, 1, conditionEnd, errors)) {
			return Invalids.STATEMENT;
		} else {
			Expression condition = ExpressionParser.parseExpression(unit, line, 2, conditionEnd, errors);
			return new WhileSt(unit.source, line[0].sourceStart, line[line.length-1].sourceStop, condition, singleLine);
		}
	}
	
	/** Assumes that the first token is KW_FOR */
	private static Statement parseForStatement(Unit unit, Token[] line, ErrorWrapper errors) {
		if(line.length < 3) {
			errors.add("Incomplete for statement:" + unit.source.getErr(line));
			return Invalids.STATEMENT;
		}
		boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
		int conditionEnd = line.length-1-(singleLine ? 0 : 1);
		if(!assertParentheses(line, 1, conditionEnd, errors))
			return Invalids.STATEMENT;
		
		int simpleRangeMarker = Utils.getTokenIdx(line, TokenBase.TK_DOUBLE_DOT, 2);
		
		if(simpleRangeMarker != -1)
			return parseRangedFor(unit, line, conditionEnd, simpleRangeMarker, singleLine, errors);
		else
			return parseComplexFor(unit, line, conditionEnd, singleLine, errors);
	}

	private static Statement parseComplexFor(Unit unit, Token[] line, int conditionEnd, boolean singleLine,
			ErrorWrapper errors) {
		VariableDeclaration declaration = null;
		Expression condition;
		AffectationSt affectation = null;
		// parse complex for
		int firstSplit = Utils.getTokenIdx(line, TokenBase.TK_COLUMN, 2);
		int secondSplit = Utils.getTokenIdx(line, TokenBase.TK_COLUMN, firstSplit+1);
		if(firstSplit == -1 || secondSplit == -1) {
			errors.add("Invalid for statement:" + unit.source.getErr(line));
			return Invalids.STATEMENT;
		}
		if(firstSplit != 2 && !Tokens.isVarType(line[2].base)) {
			errors.add("Expected variable declaration in for statement:" + unit.source.getErr(line, 2, firstSplit));
			return Invalids.STATEMENT;
		}
		if(firstSplit != 2) {
			declaration = parseVariableDeclaration(unit, Arrays.copyOfRange(line, 2, firstSplit),
					errors.subErrrors("Expected variable declaration in for statement"));
		}
		if(secondSplit != firstSplit+1) {
			condition = ExpressionParser.parseExpression(unit, line, firstSplit+1, secondSplit, 
					errors.subErrrors("Expected condition in for statement"));
		} else {
			condition = new BoolLiteral(unit.source, line[firstSplit].sourceStop, line[secondSplit].sourceStart, true);
		}
		if(conditionEnd != secondSplit+1) {
			Token[] affectationTokens = Arrays.copyOfRange(line, secondSplit+1, conditionEnd);
			ErrorWrapper subErrors = errors.subErrrors("Expected affectation in for statement");
			if(Tokens.isDirectAffectation(affectationTokens[affectationTokens.length-1].base)) {
				affectation = parseDirectAffectationStatement(unit, affectationTokens, subErrors);
			} else {
				int opPos = getAffectationTokenPos(affectationTokens);
				if(opPos != -1)
					affectation = parseAffectationStatement(unit, affectationTokens, opPos, subErrors);
				else
					subErrors.add("Not an affectation" + unit.source.getErr(affectationTokens));
			}
		}
		return new ForSt(unit.source, line[0].sourceStart, line[line.length-1].sourceStop,
				singleLine, declaration, condition, affectation);
	}

	private static Statement parseRangedFor(Unit unit, Token[] line, int conditionEnd,
			int simpleRangeMarker, boolean singleLine, ErrorWrapper errors) {
		int simpleRangeSecond = Utils.getTokenIdx(line, TokenBase.TK_DOUBLE_DOT, simpleRangeMarker+1);
		int equalsMarker = Utils.getTokenIdx(line, TokenBase.KW_EQUAL, 2);
		if(equalsMarker == -1)
			equalsMarker = Utils.getTokenIdx(line, TokenBase.TK_COLUMN, 2);
		// parse simple range for
		if(equalsMarker == -1) {
			errors.add("Invalid for-in-range declaration:" + unit.source.getErr(line));
			return Invalids.STATEMENT;
		}
		// replace ':' by '=' (necessary to parse declaration)
		line[equalsMarker] = new Token(unit.source, TokenBase.KW_EQUAL, "=", line[equalsMarker].sourceStart);
		if(line[2].base != TokenBase.TYPE_INT) {
			errors.add("Missing range target in for statement:" + unit.source.getErr(line, 2, simpleRangeMarker));
			return Invalids.STATEMENT;
		}
		VariableDeclaration declaration = parseVariableDeclaration(unit, Arrays.copyOfRange(line, 2, simpleRangeMarker), errors);
		int maxStop = simpleRangeSecond == -1 ? conditionEnd : simpleRangeSecond;
		Expression maximum = ExpressionParser.parseExpression(unit, line, simpleRangeMarker+1, maxStop, errors);
		Expression increment;
		if(simpleRangeSecond != -1)
			increment = ExpressionParser.parseExpression(unit, line, simpleRangeSecond+1, conditionEnd, errors);
		else
			increment = new IntLiteral(unit.source, line[conditionEnd].sourceStart, line[conditionEnd].sourceStart, 1);
		return new RangedForSt(unit, line[0].sourceStart, line[line.length-1].sourceStop,
				singleLine, declaration.name, declaration.getDefaultValue(), maximum, increment);
	}

	/** Assumes that the first token is KW_FOREACH */
	private static Statement parseForeachStatement(Unit unit, Token[] line, ErrorWrapper errors) {
		try {
			Pointer pointer = new Pointer();
			assertHasNext(line, pointer, "Incomplete foreach statement", errors, 5);
			boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
			int conditionEnd = line.length-1-(singleLine ? 0 : 1);
			if(!assertParentheses(line, 1, conditionEnd, errors))
				return Invalids.STATEMENT;
			if(!Tokens.isVarType(line[2].base) || line[3].base != TokenBase.VAR_VARIABLE) {
				errors.add("Expected variable declaration " + unit.source.getErr(line, 2, 3));
				return Invalids.STATEMENT;
			}
			pointer.position = 2;
			VarType type = parseType(unit, line, pointer, errors);
			assertHasNext(line, pointer, "Incomplete foreach statement", errors);
			expectToken(line[pointer.position], TokenBase.TK_COLUMN, "Expected ':'", errors);
			VariableDeclaration var = new VariableDeclaration(unit, line[2].sourceStart,
					line[pointer.position-1].sourceStop, line[3].text, type, null);
			Expression iterable = ExpressionParser.parseExpression(unit, line, pointer.position, conditionEnd, errors);
			
			return new ForEachSt(unit.source, line[0].sourceStart, line[line.length-1].sourceStop, singleLine, var, iterable);
		} catch (ParsingException e) {
			return Invalids.STATEMENT;
		}
	}
	
	/** Assumes that the first token is KW_RETURN */
	private static Statement parseReturnStatement(Unit unit, Token[] line, ErrorWrapper errors) {
		int sourceStart = line[0].sourceStart;
		int sourceStop = line[line.length-1].sourceStop;
		if(line.length == 1) {
			return new ReturnSt(unit.source, sourceStart, sourceStop);
		} else {
			Expression[] returnValues = ExpressionParser.parseArgumentList(unit, line, 1, line.length, errors);
			if(returnValues.length == 1)
				return new ReturnSt(unit.source, sourceStart, sourceStop, returnValues[0]);
			else
				return new CompositeReturnSt(unit.source, sourceStart, sourceStop, returnValues);
		}
	}
	
	/**
	 * Returns the index of the first affectation operator token (see
	 * {@link Tokens#isAffectationOperator(TokenBase)}) or {@code -1} if there is
	 * none. The first token is not accounted for so 0 cannot be returned.
	 */
	private static int getAffectationTokenPos(Token[] line) {
		for(int i = 1; i < line.length; i++) {
			if(Tokens.isAffectationOperator(line[i].base))
				return i;
		}
		return -1;
	}
	
	/**
	 * Returns {@code true} if there is an unenclosed comma before the affectation
	 * token or -1 if there is none. This method not returning -1 does NOT mean that
	 * the line is a correct affectation statement.
	 */
	private static boolean isMultipleAffectationStatement(Token[] line, int affectationTokenPos) {
		Section sec = ExpressionParser.getVisibleSection(line, 0, affectationTokenPos);
		for(int i = 0; i < affectationTokenPos; i = sec.advancePointer(i)) {
			if(line[i].base == TokenBase.TK_COMMA)
				return true;
		}
		return false;
	}
	
	/** Assumes that {@code opPos != -1} */
	private static AffectationSt parseAffectationStatement(Unit unit, Token[] line, int opPos,
			ErrorWrapper errors) {
		Expression leftOperand = ExpressionParser.parseExpression(unit, line, 0, opPos, errors);
		Expression rightOperand = ExpressionParser.parseExpression(unit, line, opPos+1, line.length, errors);
		if(line[opPos].base != TokenBase.KW_EQUAL) {
			Operator op = Tokens.getAffectationOperator(line[opPos].base);
			rightOperand = new OperationExp(unit.source, line[0].sourceStart, line[line.length-1].sourceStop,
					op, leftOperand, rightOperand);
		}
		return new AffectationSt(unit.source, line[0].sourceStart, line[line.length-1].sourceStop,
				leftOperand, rightOperand);
	}
	
	/** Assumes that {@code opPos != -1} and {@link #isMultipleAffectationStatement(Token[], int)} returned true */
	private static MultipleAffectationSt parseMultipleAffectationStatement(Unit unit, Token[] line,
			int opPos, ErrorWrapper errors) {

		String[] variables = new String[(opPos+1)/2];
		
		for(int i = 0; i < opPos; i++) {
			if(i % 2 == 0) {
				if(line[i].base != TokenBase.VAR_VARIABLE)
					errors.add("Expected variable name" + line[i].getErr());
				else
					variables[i/2] = line[i].text;
			} else if(line[i].base != TokenBase.TK_COMMA) {
				errors.add("Expected ','" + line[i].getErr());
			}
		}
		if(opPos % 2 != 1)
			errors.add("Unfinished multiple affectation statement" + line[opPos-1].getErr());
		if(line[opPos].base != TokenBase.KW_EQUAL)
			errors.add("Multiple affectations only support the '=' operator" + line[opPos].getErr());
		
		Expression[] values = ExpressionParser.parseArgumentList(unit, line, opPos+1, line.length,
				errors.subErrrors("Cannot parse affectation values"));
		
		return new MultipleAffectationSt(unit.source, line[0].sourceStart, line[line.length-1].sourceStop,
				line[opPos-1].sourceStop, variables, values);
	}
	
	private static boolean canBeFunctionStatement(Token[] line) {
		return line[line.length-1].base == TokenBase.TK_PARENTHESIS_CLOSE;
	}
	
	private static Statement parseFunctionStatement(Unit unit, Token[] line, ErrorWrapper errors) {
		Expression exp = ExpressionParser.parseExpression(unit, line, 0, line.length, errors);
		if(exp instanceof FunctionCallExp)
			return new FunctionSt(unit.source, line[0].sourceStart, line[line.length-1].sourceStop, (FunctionCallExp) exp);
		else
			throw new IllegalStateException("Statement is not a function " + unit.source.getErr(line));
	}
	
}
