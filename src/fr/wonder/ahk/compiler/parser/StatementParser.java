package fr.wonder.ahk.compiler.parser;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.FunctionCallExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.BoolLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.FloatLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.NullExp;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarGenericType;
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
import fr.wonder.ahk.compiled.statements.OperationSt;
import fr.wonder.ahk.compiled.statements.RangedForSt;
import fr.wonder.ahk.compiled.statements.ReturnSt;
import fr.wonder.ahk.compiled.statements.SectionEndSt;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.statements.WhileSt;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.GenericContext;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.parser.ExpressionParser.Section;
import fr.wonder.ahk.compiler.tokens.SectionToken;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnreachableException;

public class StatementParser extends AbstractParser {

	public static Statement parseStatement(Unit unit, Token[] line, GenericContext genc, ErrorWrapper errors) {
		return new StatementParser(unit, line, genc, errors).parseStatement();
	}
	
	public static VariableDeclaration parseVariableDeclaration(Unit unit, Token[] line, GenericContext genc, 
			DeclarationModifiers modifiers, ErrorWrapper errors) {
		return new StatementParser(unit, line, genc, errors).parseVariableDeclaration(0, line.length, modifiers, errors);
	}
	
	private Statement parseStatement() {
		
		TokenBase firstToken = line[0].base;
		switch(firstToken) {
		case KW_IF:
			return parseIfStatement();
		case KW_ELSE:
			return parseElseStatement();
		case KW_WHILE:
		case KW_DOWHILE:
			return parseWhileStatement(firstToken == TokenBase.KW_DOWHILE);
		case KW_FOR:
			return parseForStatement();
		case KW_FOREACH:
			return parseForeachStatement();
		case KW_RETURN:
			return parseReturnStatement();
		case TK_BRACE_CLOSE:
			return new SectionEndSt(line[0].sourceRef);
		case TYPE_BOOL:
		case TYPE_FLOAT:
		case TYPE_INT:
		case TYPE_STR:
		case VAR_GENERIC:
		case VAR_UNIT: // (Structure token)
			// avoid 'int:(3.) >> Kernel.out' from being treated as an int declaration
			if(line.length > 1 && line[1].base != TokenBase.TK_COMMA)
				return parseVariableDeclaration(0, line.length, DeclarationModifiers.NONE, errors);
		default:
			break;
		}
		TokenBase lastToken = line[line.length-1].base;
		int affectationTokenPos = getAffectationTokenPos(line, 0, line.length);
		if(Tokens.isDirectAffectation(lastToken))
			return parseDirectAffectationStatement(0, line.length, errors);
		if(affectationTokenPos != -1 && isMultipleAffectationStatement(affectationTokenPos))
			return parseMultipleAffectationStatement(affectationTokenPos);
		if(affectationTokenPos != -1)
			return parseAffectationStatement(0, affectationTokenPos, line.length, errors);
		if(canBeExpressionStatement())
			return tryParseExpressionStatement();
		
		errors.add("Unknown statement:" + unit.source.getErr(line));
		return Invalids.STATEMENT;
	}
	
	private final Unit unit;
	private final Token[] line;
	private final GenericContext genc;
	private final ErrorWrapper errors;
	
	private StatementParser(Unit unit, Token[] line, GenericContext genc, ErrorWrapper errors) {
		this.unit = unit;
		this.line = line;
		this.genc = genc;
		this.errors = errors;
	}

	private AffectationSt parseDirectAffectationStatement(int begin, int end, ErrorWrapper subErrors) {
		Expression leftOperand = ExpressionParser.parseExpression(unit, line, genc, begin, end-1, subErrors);
		if(!subErrors.noErrors())
			return Invalids.AFFECTATION_STATEMENT;
		Token last = line[end-1];
		Expression rightOperand = new IntLiteral(last.sourceRef, 1);
		Operator op = Tokens.getDirectOperation(last.base);
		Expression affectation = new OperationExp(SourceReference.fromLine(line, begin, end-1), op, leftOperand, rightOperand);
		return new AffectationSt(SourceReference.fromLine(line, begin, end-1), leftOperand, affectation);
	}
	
	private VariableDeclaration parseVariableDeclaration(int begin, int end, DeclarationModifiers modifiers, ErrorWrapper subErrors) {
		
		try {
			Pointer pointer = new Pointer(begin);
			
			assertHasNext(line, pointer, "Invalid variable declaration", subErrors);
			
			VarType type = parseType(unit, line, genc, pointer, ALLOW_NONE, subErrors);
			
			String varName = assertToken(line, pointer, TokenBase.VAR_VARIABLE, "Expected variable name", subErrors).text;
			
			Expression defaultValue;
			if(pointer.position == end) {
				defaultValue = getDefaultValue(type, unit.source, line[end-1].sourceRef.stop);
			} else {
				assertToken(line, pointer, TokenBase.KW_EQUAL, "Expected affectation value", subErrors);
				assertHasNext(line, pointer, "Expected affectation value", subErrors);
				defaultValue = ExpressionParser.parseExpression(unit, line, genc, pointer.position, end, subErrors);
			}
			
			return new VariableDeclaration(unit, SourceReference.fromLine(line),
					varName, type, modifiers, defaultValue);
		} catch (ParsingException e) {
			return Invalids.VARIABLE_DECLARATION;
		}
	}
	
	public static Expression getDefaultValue(VarType type, UnitSource source, int sourceLoc) {
		SourceReference sourceRef = new SourceReference(source, sourceLoc, sourceLoc);
		if(type == VarType.INT)
			return new IntLiteral(sourceRef, 0);
		else if(type == VarType.FLOAT)
			return new FloatLiteral(sourceRef, 0);
		else if(type == VarType.BOOL)
			return new BoolLiteral(sourceRef, false);
		else if(type == VarType.STR || type instanceof VarStructType)
			return new NullExp(sourceRef);
		else if(type == Invalids.TYPE)
			return Invalids.EXPRESSION;
		else if(type instanceof VarArrayType || type instanceof VarGenericType || type instanceof VarFunctionType)
			return new NullExp(sourceRef);
		else
			throw new UnreachableException("Unimplemented type default value for " + type);
	}
	
	/** Assumes that the first token is KW_IF */
	private Statement parseIfStatement() {
		try {
			assertHasNext(line, new Pointer(), "Incomplete if statement", errors, 3);
			boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
			int conditionEnd = line.length-1-(singleLine ? 0 : 1);
			assertParentheses(line, 1, conditionEnd, errors);
			Expression condition = ExpressionParser.parseExpression(unit, line, genc, 2, conditionEnd, errors);
			return new IfSt(SourceReference.fromLine(line), condition, singleLine);
		} catch (ParsingException e) {
			return Invalids.STATEMENT;
		}
	}
	
	/** Assumes that the first token is KW_ELSE */
	private Statement parseElseStatement() {
		boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
		return new ElseSt(SourceReference.fromLine(line), singleLine);
	}
	
	/** Assumes that the first token is KW_WHILE or KW_DOWHILE */
	private Statement parseWhileStatement(boolean isDoWhile) {
		try {
			assertHasNext(line, new Pointer(), "Incomplete while statement", errors, 3);
			boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
			int conditionEnd = line.length-1-(singleLine ? 0 : 1);
			assertParentheses(line, 1, conditionEnd, errors);
			Expression condition = ExpressionParser.parseExpression(unit, line, genc, 2, conditionEnd, errors);
			return new WhileSt(SourceReference.fromLine(line), condition, singleLine, isDoWhile);
		} catch (ParsingException e) {
			return Invalids.STATEMENT;
		}
	}
	
	/** Assumes that the first token is KW_FOR */
	private Statement parseForStatement() {
		try {
			assertHasNext(line, new Pointer(), "Incomplete for statement", errors, 3);
			boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
			int conditionEnd = line.length-1-(singleLine ? 0 : 1);
			assertParentheses(line, 1, conditionEnd, errors);
			int simpleRangeMarker = Utils.getTokenIdx(line, TokenBase.TK_DOUBLE_DOT, 2);
			if(simpleRangeMarker != -1)
				return parseRangedFor(conditionEnd, simpleRangeMarker, singleLine);
			else
				return parseComplexFor(conditionEnd, singleLine);
		} catch (ParsingException e) {
			return Invalids.STATEMENT;
		}
	}

	private Statement parseComplexFor(int conditionEnd, boolean singleLine) {
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
			declaration = parseVariableDeclaration(2, firstSplit, DeclarationModifiers.NONE,
					errors.subErrors("Expected variable declaration in for statement"));
		}
		if(secondSplit != firstSplit+1) {
			condition = ExpressionParser.parseExpression(unit, line, genc, firstSplit+1, secondSplit, 
					errors.subErrors("Expected condition in for statement"));
		} else {
			SourceReference sourceRef = SourceReference.fromLine(line, firstSplit, secondSplit);
			condition = new BoolLiteral(sourceRef, true);
		}
		if(conditionEnd != secondSplit+1) {
			ErrorWrapper subErrors = errors.subErrors("Expected affectation in for statement");
			if(Tokens.isDirectAffectation(line[conditionEnd-1].base)) {
				affectation = parseDirectAffectationStatement(secondSplit+1, conditionEnd, subErrors);
			} else {
				int opPos = getAffectationTokenPos(line, secondSplit+1, conditionEnd);
				if(opPos != -1)
					affectation = parseAffectationStatement(secondSplit+1, opPos, conditionEnd, subErrors);
				else
					subErrors.add("Not an affectation:" + unit.source.getErr(line, secondSplit+1, conditionEnd));
			}
		}
		return new ForSt(SourceReference.fromLine(line),
				singleLine, declaration, condition, affectation);
	}

	@SuppressWarnings("unchecked")
	private Statement parseRangedFor(int conditionEnd, int simpleRangeMarker, boolean singleLine) {
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
		line[equalsMarker] = new Token(line[equalsMarker].sourceRef, TokenBase.KW_EQUAL, "=");
		if(line[2].base != TokenBase.TYPE_INT) {
			errors.add("Missing range target in for statement:" + unit.source.getErr(line, 2, simpleRangeMarker));
			return Invalids.STATEMENT;
		}
		VariableDeclaration declaration = parseVariableDeclaration(2, simpleRangeMarker, DeclarationModifiers.NONE, errors);
		int maxBegin = simpleRangeSecond == -1 ? simpleRangeMarker+1 : simpleRangeSecond+1;
		Expression maximum = ExpressionParser.parseExpression(unit, line, genc, maxBegin, conditionEnd, errors);
		LiteralExp<? extends Number> step;
		if(simpleRangeSecond != -1) {
			Expression increment = ExpressionParser.parseExpression(unit, line, genc, simpleRangeMarker+1, simpleRangeSecond, errors);
			if(!(increment instanceof IntLiteral) && !(increment instanceof FloatLiteral)) {
				errors.add("Invalid step in for statement, expected float or int literal:" + increment.getErr());
				return Invalids.STATEMENT;
			}
			step = (LiteralExp<? extends Number>) increment;
			if(step.value.doubleValue() == 0) {
				errors.add("Invalid step in for statement, the step cannot be zero:" + increment.getErr());
				return Invalids.STATEMENT;
			}
		} else {
			step = new IntLiteral(line[conditionEnd].sourceRef, 1);
		}
		return new RangedForSt(unit, SourceReference.fromLine(line),
				singleLine, declaration.name, declaration.getDefaultValue(), maximum, step);
	}

	/** Assumes that the first token is KW_FOREACH */
	private Statement parseForeachStatement() {
		try {
			Pointer pointer = new Pointer();
			assertHasNext(line, pointer, "Incomplete foreach statement", errors, 5);
			boolean singleLine = line[line.length-1].base != TokenBase.TK_BRACE_OPEN;
			int conditionEnd = line.length-1-(singleLine ? 0 : 1);
			assertParentheses(line, 1, conditionEnd, errors);
			if(!Tokens.isVarType(line[2].base) || line[3].base != TokenBase.VAR_VARIABLE) {
				errors.add("Expected variable declaration " + unit.source.getErr(line, 2, 3));
				return Invalids.STATEMENT;
			}
			pointer.position = 2;
			VarType type = parseType(unit, line, genc, pointer, ALLOW_NONE, errors);
			assertHasNext(line, pointer, "Incomplete foreach statement", errors);
			assertToken(line, pointer, TokenBase.TK_COLUMN, "Expected ':'", errors);
			VariableDeclaration var = new VariableDeclaration(unit,
					SourceReference.fromLine(line, 2, pointer.position-1),
					line[3].text, type, DeclarationModifiers.NONE, null);
			Expression iterable = ExpressionParser.parseExpression(
					unit, line, genc, pointer.position, conditionEnd, errors);
			
			return new ForEachSt(SourceReference.fromLine(line),
					singleLine, var, iterable);
		} catch (ParsingException e) {
			return Invalids.STATEMENT;
		}
	}
	
	/** Assumes that the first token is KW_RETURN */
	private Statement parseReturnStatement() {
		SourceReference sourceRef = SourceReference.fromLine(line);
		if(line.length == 1) {
			return new ReturnSt(sourceRef);
		} else {
			Expression[] returnValues = ExpressionParser.parseArgumentList(unit, line, genc, 1, line.length, errors);
			if(returnValues.length == 1)
				return new ReturnSt(sourceRef, returnValues[0]);
			else
				return new CompositeReturnSt(sourceRef, returnValues);
		}
	}
	
	/**
	 * Returns the index of the first affectation operator token (see
	 * {@link Tokens#isAffectationOperator(TokenBase)}) or {@code -1} if there is
	 * none. The first token is not accounted for so 0 cannot be returned.
	 */
	private static int getAffectationTokenPos(Token[] line, int begin, int end) {
		for(int i = begin+1; i < end; i++) {
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
	private boolean isMultipleAffectationStatement(int affectationTokenPos) {
		Section sec = ExpressionParser.getVisibleSection(line, 0, affectationTokenPos);
		for(Pointer p = new Pointer(); p.position < affectationTokenPos; sec.advancePointer(p)) {
			if(line[p.position].base == TokenBase.TK_COMMA)
				return true;
		}
		return false;
	}
	
	/** Assumes that {@code opPos != -1} */
	private AffectationSt parseAffectationStatement(int variableBegin, int opPos, int valueEnd, ErrorWrapper subErrors) {
		Expression leftOperand = ExpressionParser.parseExpression(unit, line, genc, variableBegin, opPos, subErrors);
		Expression rightOperand = ExpressionParser.parseExpression(unit, line, genc, opPos+1, valueEnd, subErrors);
		if(line[opPos].base != TokenBase.KW_EQUAL) {
			Operator op = Tokens.getAffectationOperator(line[opPos].base);
			rightOperand = new OperationExp(SourceReference.fromLine(line, variableBegin, valueEnd-1),
					op, leftOperand, rightOperand);
		}
		return new AffectationSt(SourceReference.fromLine(line),
				leftOperand, rightOperand);
	}
	
	/** Assumes that {@code opPos != -1} and {@link #isMultipleAffectationStatement(Token[], int)} returned true */
	private MultipleAffectationSt parseMultipleAffectationStatement(int opPos) {
		if(line[opPos].base != TokenBase.KW_EQUAL)
			errors.add("Multiple affectations only support the '=' operator" + line[opPos].getErr());
		
		Expression[] variables = ExpressionParser.parseArgumentList(unit, line, genc, 0, opPos,
				errors.subErrors("Cannot parse affectation variables"));
		Expression[] values = ExpressionParser.parseArgumentList(unit, line, genc, opPos+1, line.length,
				errors.subErrors("Cannot parse affectation values"));
		
		return new MultipleAffectationSt(SourceReference.fromLine(line),
				line[opPos-1].sourceRef.stop, variables, values);
	}
	
	private Statement tryParseExpressionStatement() {
		Expression statementAsExpression = ExpressionParser.parseExpression(unit, line, genc, 0, line.length, errors);
		if(statementAsExpression instanceof FunctionCallExp)
			return new FunctionSt(statementAsExpression.sourceRef, (FunctionCallExp) statementAsExpression);
		if(statementAsExpression instanceof OperationExp)
			return new OperationSt(statementAsExpression.sourceRef, (OperationExp) statementAsExpression);
		errors.add("Only function calls and operations expressions can be valid statements:" + unit.source.getErr(line));
		return null;
	}
	
	private boolean canBeExpressionStatement() {
		if(line[line.length-1].base == TokenBase.TK_BRACE_OPEN)
			return false;
		
		Section sec = ExpressionParser.getVisibleSection(line, 0, line.length);
		// line may be an overloaded operator function call
		if(!sec.operators.isEmpty())
			return true;
		// line may be a function call
		for(Section subSection : sec.subsections)
			if(subSection.type == SectionToken.SEC_PARENTHESIS)
				return true;
		return false;
	}
	
}
