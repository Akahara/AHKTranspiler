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

public class StatementParser {

	public static Statement parseStatement(Unit unit, Token[] line, ErrorWrapper errors) {
		
		TokenBase firstToken = line[0].base;
		switch(firstToken) {
		case KW_IF:
			return parseIfStatement(unit.source, line, errors);
		case KW_ELSE:
			return parseElseStatement(unit.source, line, errors);
		case KW_WHILE:
			return parseWhileStatement(unit.source, line, errors);
		case KW_FOR:
			return parseForStatement(unit, line, errors);
		case KW_FOREACH:
			return parseForeachStatement(unit, line, errors);
		case KW_RETURN:
			return parseReturnStatement(unit.source, line, errors);
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
			return parseDirectAffectationStatement(unit.source, line, errors);
		if(affectationTokenPos != -1 && isMultipleAffectationStatement(line, affectationTokenPos))
			return parseMultipleAffectationStatement(unit.source, line, affectationTokenPos, errors);
		if(affectationTokenPos != -1)
			return parseAffectationStatement(unit.source, line, affectationTokenPos, errors);
		if(isFunctionStatement(line))
			return parseFunctionStatement(unit.source, line, errors);
		
		errors.add("Unknown statement:" + unit.source.getErr(line));
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
	public static VariableDeclaration parseVariableDeclaration(Unit unit, Token[] line, ErrorWrapper errors) {
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
		VarType varType = Tokens.getType(unit, line[0]);
		
		if(varType == null) {
			subErrors.add("Unknown value type:" + line[0].getErr());
			varType = Invalids.TYPE;
		}
		
		for(int i = 0; i < arrayDimensions; i++)
			varType = new VarArrayType(varType);
		
		Expression defaultValue;
		if(line.length == t+1) {
			defaultValue = getDefaultValue(varType, unit.source, line[t].sourceStop);
		} else {
			defaultValue = ExpressionParser.parseExpression(unit.source, line, t+2, line.length, subErrors);
		}
		
		return new VariableDeclaration(unit.source, line[0].sourceStart, line[line.length-1].sourceStop, varName, varType, defaultValue);
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
			condition = ExpressionParser.parseExpression(unit.source, line, firstSplit+1, secondSplit, 
					errors.subErrrors("Expected condition in for statement"));
		} else {
			condition = new BoolLiteral(unit.source, line[firstSplit].sourceStop, line[secondSplit].sourceStart, true);
		}
		if(conditionEnd != secondSplit+1) {
			Token[] affectationTokens = Arrays.copyOfRange(line, secondSplit+1, conditionEnd);
			ErrorWrapper subErrors = errors.subErrrors("Expected affectation in for statement");
			if(Tokens.isDirectAffectation(affectationTokens[affectationTokens.length-1].base)) {
				affectation = parseDirectAffectationStatement(unit.source, affectationTokens, subErrors);
			} else {
				int opPos = getAffectationTokenPos(affectationTokens);
				if(opPos != -1)
					affectation = parseAffectationStatement(unit.source, affectationTokens, opPos, subErrors);
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
		Expression maximum = ExpressionParser.parseExpression(unit.source, line, simpleRangeMarker+1, maxStop, errors);
		Expression increment;
		if(simpleRangeSecond != -1)
			increment = ExpressionParser.parseExpression(unit.source, line, simpleRangeSecond+1, conditionEnd, errors);
		else
			increment = new IntLiteral(unit.source, line[conditionEnd].sourceStart, line[conditionEnd].sourceStart, 1);
		return new RangedForSt(unit.source, line[0].sourceStart, line[line.length-1].sourceStop,
				singleLine, declaration.name, declaration.getDefaultValue(), maximum, increment);
	}

	/** Assumes that the first token is KW_FOREACH */
	private static Statement parseForeachStatement(Unit unit, Token[] line, ErrorWrapper errors) {
		if(line.length < 5) {
			errors.add("Incomplete foreach statement" + unit.source.getErr(line));
			return Invalids.STATEMENT;
		}
		boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
		int conditionEnd = line.length-1-(singleLine ? 0 : 1);
		if(!assertParentheses(line, 1, conditionEnd, errors))
			return Invalids.STATEMENT;
		if(!Tokens.isVarType(line[2].base) || line[3].base != TokenBase.VAR_VARIABLE) {
			errors.add("Expected variable declaration " + unit.source.getErr(line, 2, 3));
			return Invalids.STATEMENT;
		}
		if(line[4].base != TokenBase.TK_COLUMN) {
			errors.add("Expected ':' at" + line[4].getErr());
			return Invalids.STATEMENT;
		}
		VariableDeclaration var = new VariableDeclaration(unit.source, line[2].sourceStart, line[3].sourceStop,
				line[3].text, Tokens.getType(unit, line[2]), null);
		Expression iterable = ExpressionParser.parseExpression(unit.source, line, 5, conditionEnd, errors);
		
		return new ForEachSt(unit.source, line[0].sourceStart, line[line.length-1].sourceStop, singleLine, var, iterable);
	}
	
	/** Assumes that the first token is KW_RETURN */
	private static Statement parseReturnStatement(UnitSource unit, Token[] line, ErrorWrapper errors) {
		int sourceStart = line[0].sourceStart;
		int sourceStop = line[line.length-1].sourceStop;
		if(line.length == 1) {
			return new ReturnSt(unit, sourceStart, sourceStop);
		} else {
			Expression[] returnValues = ExpressionParser.parseArgumentList(unit, line, 1, line.length, errors);
			if(returnValues.length == 1)
				return new ReturnSt(unit, sourceStart, sourceStop, returnValues[0]);
			else
				return new CompositeReturnSt(unit, sourceStart, sourceStop, returnValues);
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
	private static AffectationSt parseAffectationStatement(UnitSource source, Token[] line, int opPos,
			ErrorWrapper errors) {
		Expression leftOperand = ExpressionParser.parseExpression(source, line, 0, opPos, errors);
		Expression rightOperand = ExpressionParser.parseExpression(source, line, opPos+1, line.length, errors);
		if(line[opPos].base != TokenBase.KW_EQUAL) {
			Operator op = Tokens.getAffectationOperator(line[opPos].base);
			rightOperand = new OperationExp(source, line[0].sourceStart, line[line.length-1].sourceStop,
					op, leftOperand, rightOperand);
		}
		return new AffectationSt(source, line[0].sourceStart, line[line.length-1].sourceStop,
				leftOperand, rightOperand);
	}
	
	/** Assumes that {@code opPos != -1} and {@link #isMultipleAffectationStatement(Token[], int)} returned true */
	private static MultipleAffectationSt parseMultipleAffectationStatement(UnitSource source, Token[] line,
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
		
		Expression[] values = ExpressionParser.parseArgumentList(source, line, opPos+1, line.length,
				errors.subErrrors("Cannot parse affectation values"));
		
		return new MultipleAffectationSt(source, line[0].sourceStart, line[line.length-1].sourceStop,
				line[opPos-1].sourceStop, variables, values);
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
