package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.ArrayExp;
import fr.wonder.ahk.compiled.expressions.ConstructorExp;
import fr.wonder.ahk.compiled.expressions.ConversionExp;
import fr.wonder.ahk.compiled.expressions.DirectAccessExp;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.FunctionCallExp;
import fr.wonder.ahk.compiled.expressions.IndexingExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.LiteralExp.BoolLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.EnumLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.FloatLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.IntLiteral;
import fr.wonder.ahk.compiled.expressions.LiteralExp.StrLiteral;
import fr.wonder.ahk.compiled.expressions.NullExp;
import fr.wonder.ahk.compiled.expressions.OperationExp;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.SimpleLambdaExp;
import fr.wonder.ahk.compiled.expressions.SizeofExp;
import fr.wonder.ahk.compiled.expressions.UninitializedArrayExp;
import fr.wonder.ahk.compiled.expressions.VarExp;
import fr.wonder.ahk.compiled.expressions.types.EnumValue;
import fr.wonder.ahk.compiled.expressions.types.VarEnumType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.LambdaClosureArgument;
import fr.wonder.ahk.compiled.units.sections.SimpleLambda;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.tokens.SectionToken;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnreachableException;
import fr.wonder.commons.types.Tuple;

public class ExpressionParser extends AbstractParser {
	
	static class Section {
		
		SectionToken type;
		/** Inner positions (line[start|stop] won't likely be a parenthesis) */
		int start, stop;
		List<Section> subsections = new ArrayList<>();
		/** Operator - Position tuples */
		List<Tuple<Operator, Integer>> operators = new ArrayList<>();
		
		Section(SectionToken type, int start, int stop) {
			this.type = type;
			this.start = start;
			this.stop = stop;
		}
		
		Section(int start, int stop) {
			this.start = start;
			this.stop = stop;
		}
		
		@Override
		public String toString() {
			return type+"["+start+":"+stop+"]";
		}
		
		Section getSubSection(int start, int stop) {
			Section subSection = new Section(start, stop);
			for(Section sec : subsections) {
				if(sec.start == start && sec.stop == stop)
					return sec;
				if(sec.start < stop && sec.stop >= start)
					subSection.subsections.add(sec);
				if((sec.start < stop && sec.stop > stop) || (sec.stop > start && sec.start < start))
					throw new UnreachableException("Unexpected section overlap");
			}
			for(Tuple<Operator, Integer> op : operators) {
				if(start <= op.b && op.b < stop)
					subSection.operators.add(op);
			}
			return subSection;
		}
		
		Section lastSubsection() {
			return subsections.get(subsections.size()-1);
		}
		
		Section firstSubsection() {
			return subsections.get(0);
		}
		
		void advancePointer(Pointer p) {
			boolean updated;
			p.position++;
			do {
				updated = false;
				for(Section s : subsections) {
					if(s.start-1 == p.position) {
						p.position = s.stop+1;
						updated = true;
						break;
					}
				}
			} while(updated);
		}

		public Pointer makePointer() {
			Pointer p = new Pointer(start-1);
			advancePointer(p);
			return p;
		}
		
	}

	static Section getVisibleSection(Token[] line, int start, int stop) {
		List<Section> sections = new ArrayList<>();
		Section current = new Section(start, stop);
		
		for(int i = start; i < stop; i++) {
			TokenBase t = line[i].base;
			
			if(Tokens.isOperator(t)) {
				current.operators.add(new Tuple<>(Tokens.getOperator(t), i));
				continue;
			}
			
			for(SectionToken sec : Tokens.CODE_SECTIONS) {
				if(t == sec.start) {
					Section subSection = new Section(sec, i+1, -1);
					current.subsections.add(subSection);
					sections.add(current);
					current = subSection;
					break;
				} else if(t == sec.stop) {
					if(current.type != sec)
						throw new IllegalStateException("Unexpected section closing: " +
								sec + line[0].sourceRef.source.getErr(line));
					current.stop = i;
					current = sections.remove(sections.size()-1);
					break;
				}
			}
		}
		if(!sections.isEmpty())
			throw new IllegalArgumentException("Unexpected unclosed section: " +
					sections.get(sections.size()-1) + line[0].sourceRef.source.getErr(line));
		return current;
	}
	
	public static Expression parseExpression(Unit unit, Token[] line, int start, int stop, ErrorWrapper errors) {
		return new ExpressionParser(unit, line, errors).parseExpression(getVisibleSection(line, start, stop));
	}
	
	public static Expression[] parseArgumentList(Unit unit, Token[] line, int start, int stop, ErrorWrapper errors) {
		return new ExpressionParser(unit, line, errors).parseArgumentList(getVisibleSection(line, start, stop));
	}
	
	
	private final Unit unit;
	private final Token[] line;
	private final ErrorWrapper errors;
	
	private ExpressionParser(Unit unit, Token[] line, ErrorWrapper errors) {
		this.unit = unit;
		this.line = line;
		this.errors = errors;
	}
	
	private SourceReference sourceRefOfSection(Section section) {
		return SourceReference.fromLine(line, section.start, section.stop-1);
	}
	
	private Expression withError(String error) {
		errors.add(error);
		return Invalids.EXPRESSION;
	}
	
	private String getSectionErr(Section section, boolean whole) {
		return unit.source.getErr(line, section.start + (whole ? -1 : 0), section.stop + (whole ? 1 : 0));
	}
	
	private Expression parseExpression(Section section) {
//		Utils.dump(line, section.start, section.stop);
		if(section.stop == section.start)
			return withError("Empty expression:" + line[section.start].getErr());
		
		// remove extra parenthesis
		{
			Section firstSection = section.subsections.isEmpty() ? null : section.subsections.get(0);
			while(section.subsections.size() == 1 && firstSection.start == section.start+1 && 
					firstSection.stop == section.stop-1 && firstSection.type == SectionToken.SEC_PARENTHESIS) {
				section = firstSection;
				firstSection = section.subsections.isEmpty() ? null : section.subsections.get(0);
			}
		}
		
		// parse single token expression
		if(section.stop-section.start == 1) {
			Token tk = line[section.start];
			if(tk.base == TokenBase.LIT_NULL)
				return new NullExp(tk.sourceRef);
			else if(tk.base == TokenBase.VAR_VARIABLE)
				return new VarExp(tk.sourceRef, line[section.start].text);
			else if(Tokens.isLiteral(tk.base))
				return parseLiteral(line[section.start], errors);
			else if(tk.base == TokenBase.VAR_ENUM)
				return parseEnumLiteral(line[section.start]);
			return withError("Unknown expression type" + tk.getErr());
		}
		
		// parse lambdas
		int lambdaOperatorIndex = findLambdaOperatorIndex(section);
		if(lambdaOperatorIndex != -1)
			return parseLambdaExpression(section, lambdaOperatorIndex);
		
		// parse operation
		if(!section.operators.isEmpty())
			return parseOperationExpression(section);

		Section lastSection = section.subsections.isEmpty() ? null : section.lastSubsection();
		
		// parse conversion expression
		if(section.subsections.size() == 1 && lastSection.type == SectionToken.SEC_PARENTHESIS &&
				lastSection.start == section.start+3 && line[section.start+1].base == TokenBase.TK_COLUMN) {
			VarType type = parseType(unit, line, new Pointer(section.start), errors);
			Expression casted = parseExpression(lastSection);
			return new ConversionExp(sourceRefOfSection(section), type, casted, false);
		}
		
		// parse sizeof
		if(line[section.start].base == TokenBase.KW_SIZEOF) {
			Expression exp = parseExpression(section.getSubSection(section.start+1, section.stop));
			return new SizeofExp(sourceRefOfSection(section), exp);
		}
		
		// parse direct access exp
		if(section.stop-section.start >= 3 && line[section.stop-2].base == TokenBase.TK_DOT) {
			return parseDirectAccessExpression(section);
		}
		
		// parse function and arrays
		if(lastSection != null && lastSection.stop == section.stop - 1) {
			if(lastSection.type == SectionToken.SEC_PARENTHESIS) {
				if(line[section.start].base == TokenBase.VAR_UNIT)
					return parseConstructorExpression(section);
				else
					return parseFunctionExpression(section);
			} else if(lastSection.type == SectionToken.SEC_BRACKETS) {
				if(section.start+1 == lastSection.start)
					return parseArrayExpression(section);
				else
					return parseIndexingExpression(section);
			}
			throw new UnreachableException("Invalid section type " + lastSection.type);
		}
		
		return withError("Unknown expression type:" + sourceRefOfSection(section).getErr());
	}

	public static LiteralExp<?> parseLiteral(Token t, ErrorWrapper errors) {
		switch(t.base) {
		case LIT_INT:
			try {
				return new IntLiteral(t.sourceRef, Long.parseLong(t.text));
			} catch (NumberFormatException e) {
				errors.add("Unable to parse int literal: " + e.getMessage() + t.getErr());
				return Invalids.LITERAL_EXPRESSION;
			}
		case LIT_FLOAT:
			try {
				return new FloatLiteral(t.sourceRef, Double.parseDouble(t.text));
			} catch (NumberFormatException e) {
				errors.add("Unable to parse float literal: " + e.getMessage() + t.getErr());
				return Invalids.LITERAL_EXPRESSION;
			}
		case LIT_STR:
			return new StrLiteral(t.sourceRef, t.text);
		case LIT_BOOL_TRUE:
			return new BoolLiteral(t.sourceRef, true);
		case LIT_BOOL_FALSE:
			return new BoolLiteral(t.sourceRef, false);
		default:
			errors.add("Unable to parse literal: Token is not a literal value" + t.getErr());
			return Invalids.LITERAL_EXPRESSION;
		}
	}
	
	/** Assumes that tokenBase == VAR_ENUM */
	private Expression parseEnumLiteral(Token token) {
		String enumValue = Tokens.extractEnumValue(token);
		VarEnumType enumType = unit.getEnumType(token);
		EnumValue value = new EnumValue(enumType, enumValue);
		return new EnumLiteral(token.sourceRef, enumType, value);
	}

	/** Assumes that section.operators is not empty */
	private Expression parseOperationExpression(Section section) {
		Tuple<Operator, Integer> operator = section.operators.get(0);
		// Tuple.second is the operator position in the line
		
		for(int i = 1; i < section.operators.size(); i++) {
			Tuple<Operator, Integer> op = section.operators.get(i);
			if(op.a.priority <= operator.a.priority)
				operator = op;
		}
		Token opt = line[operator.b];
		Expression leftOperand = null;
		Expression rightOperand = null;
		if(operator.b == section.start) {
			if(!operator.a.doesSingleOperand)
				errors.add("Operator " + opt.text + " does not allow for single operand operations" + opt.getErr());
		} else {
			leftOperand = parseExpression(section.getSubSection(section.start, operator.b));
		}
		if(operator.b+1 == section.stop) {
			errors.add("Operator " + opt.text + " does not have a right operand" + opt.getErr());
		} else {
			rightOperand = parseExpression(section.getSubSection(operator.b+1, section.stop));
		}
		
		if(leftOperand == null) {
			if(operator.a == Operator.SUBSTRACT) {
				if(rightOperand instanceof IntLiteral)
					return new IntLiteral(SourceReference.concat(opt, rightOperand), -((IntLiteral) rightOperand).value);
				else if(rightOperand instanceof FloatLiteral)
					return new FloatLiteral(SourceReference.concat(opt, rightOperand), -((FloatLiteral) rightOperand).value);
			}
			
			return new OperationExp(sourceRefOfSection(section), operator.a, rightOperand);
		} else {
			return new OperationExp(sourceRefOfSection(section), operator.a, leftOperand, rightOperand);
		}
	}
	
	private Expression[] parseArgumentList(Section section) {
		List<Expression> arguments = new ArrayList<>();
		if(section.stop-section.start != 0) {
			int last = section.start;
			for(Pointer p = new Pointer(section.start); p.position < section.stop; section.advancePointer(p)) {
				if(line[p.position].base == TokenBase.TK_COMMA) {
					if(last == p.position)
						errors.add("Unexpected ',' in argument list" + line[p.position].getErr());
					else
						arguments.add(parseExpression(section.getSubSection(last, p.position)));
					last = p.position+1;
				}
			}
			if(last == section.stop)
				errors.add("Missing tailing expression in argument list" + line[last-1].getErr());
			else
				arguments.add(parseExpression(section.getSubSection(last, section.stop)));
		}
		return arguments.toArray(Expression[]::new);
	}
	
	/** Assumes that the section has its size >= 3 and that the second-to-last token is a dot */
	private Expression parseDirectAccessExpression(Section section) {
		Token memberToken = line[section.stop-1];
		if(memberToken.base != TokenBase.VAR_VARIABLE) {
			return withError("Expected a struct member name:" + memberToken.getErr());
		}
		Expression structInstance = parseExpression(section.getSubSection(section.start, section.stop-2));
		String memberName = memberToken.text;
		return new DirectAccessExp(sourceRefOfSection(section), structInstance, memberName);
	}
	
	/** Assumes that the last subsection is a parenthesis section */
	private Expression parseFunctionExpression(Section section) {
		Section parenthesis = section.subsections.get(section.subsections.size()-1);
		Expression[] arguments = parseArgumentList(parenthesis);
		Expression function = parseExpression(section.getSubSection(section.start, parenthesis.start-1));
		return new FunctionCallExp(sourceRefOfSection(section), function, arguments);
	}
	
	/** Assumes that the section only contains a single parenthesis sub-section */
	private Expression parseConstructorExpression(Section section) {
		Section argsSection = section.lastSubsection();
		Expression[] arguments = parseArgumentList(argsSection);
		Pointer p = new Pointer(section.start);
		VarType structType = unit.getAliasOrStructType(line[p.position++]);
		if(p.position != argsSection.start-1)
			return withError("Unexpected tokens:" + unit.source.getErr(line, p.position, argsSection.start-1));
		return new ConstructorExp(sourceRefOfSection(section), structType, arguments);
	}
	
	/** Assumes that the last subsection is a bracket section */
	private Expression parseIndexingExpression(Section section) {
		Section brackets = section.lastSubsection();
		Expression[] arguments = parseArgumentList(brackets);
		if(arguments.length == 0)
			errors.add("Empty index:" + sourceRefOfSection(brackets).getErr());
		Expression array = parseExpression(section.getSubSection(section.start, brackets.start-1));
		return new IndexingExp(sourceRefOfSection(section), array, arguments);
	}
	
	private Expression parseArrayExpression(Section section) {
		Section brackets = section.lastSubsection();
		if(line[brackets.start].base == TokenBase.TK_COLUMN) {
			Expression size = parseExpression(brackets.getSubSection(brackets.start+1, brackets.stop));
			return new UninitializedArrayExp(sourceRefOfSection(section), size);
		} else {
			Expression[] values = parseArgumentList(brackets);
			return new ArrayExp(sourceRefOfSection(section), values);
		}
	}
	
	private int findLambdaOperatorIndex(Section section) {
		for(Pointer p = section.makePointer(); p.position < section.stop; section.advancePointer(p)) {
			if(line[p.position].base == TokenBase.TK_LAMBDA_ACTION)
				return p.position;
		}
		return -1;
	}
	
	private LambdaClosureArgument[] parseClosureArgumentsList(Section section, Pointer p) throws ParsingException {
		List<LambdaClosureArgument> arguments = new ArrayList<>();
		assertToken(line, p, TokenBase.TK_BRACKET_OPEN, "Expected '[' to begin closure arguments", errors);
		if(line[p.position].base != TokenBase.TK_BRACE_CLOSE) {
			while(true) {
				Token argToken = assertToken(line, p, TokenBase.VAR_VARIABLE, "Expected closure argument name", errors);
				String argName = argToken.text;
				arguments.add(new LambdaClosureArgument(argToken.sourceRef, argName));
				assertHasNext(line, p, "Expected closure argument list", errors);
				if(line[p.position].base != TokenBase.TK_COMMA)
					break;
				p.position++;
			}
		}
		assertToken(line, p, TokenBase.TK_BRACKET_CLOSE, "Expected ']' to end closure arguments", errors);
		return arguments.toArray(LambdaClosureArgument[]::new);
	}
	
	private Expression parseLambdaExpression(Section section, int lambdaOperatorIndex) {
		Section argsParentheses = section.firstSubsection();
		if(argsParentheses == null || argsParentheses.start > lambdaOperatorIndex)
			return withError("Expected lambda type" + line[lambdaOperatorIndex].getErr());
		if(argsParentheses.type != SectionToken.SEC_PARENTHESIS)
			return withError("Expected lambda type" + getSectionErr(argsParentheses, true));
		if(argsParentheses.start != section.start+1)
			return withError("Unexpected tokens before lambda" + unit.source.getErr(line, section.start, argsParentheses.start-1));
		ArgumentList args;
		VarType returnType;
		LambdaClosureArgument[] closureArguments;
		try {
			Pointer p = new Pointer(argsParentheses.start-1);
			args = readArguments(unit, line, true, p, errors);
			TokenBase nextToken = line[p.position].base;
			if(nextToken == TokenBase.TK_BRACKET_OPEN)
				closureArguments = parseClosureArgumentsList(section.subsections.get(1), p);
			else
				closureArguments = LambdaClosureArgument.NO_ARGUMENTS;
			assertToken(line, p, TokenBase.TK_COLUMN, "Expected return type of lambda", errors);
			returnType = parseType(unit, line, p, errors);
			assertToken(line, p, TokenBase.TK_LAMBDA_ACTION, "Unexpected token after lambda type", errors);
		} catch (ParsingException x) {
			return Invalids.EXPRESSION;
		}
		Expression body = parseExpression(section.getSubSection(lambdaOperatorIndex+1, section.stop));
		SourceReference lambdaSourceRef = sourceRefOfSection(section);
		SimpleLambda lambda = new SimpleLambda(lambdaSourceRef, returnType, args.asArray(), closureArguments, body);
		unit.lambdas.add(lambda);
		return new SimpleLambdaExp(lambdaSourceRef, lambda);
	}
	
}
