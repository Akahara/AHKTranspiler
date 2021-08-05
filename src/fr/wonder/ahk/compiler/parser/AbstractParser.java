package fr.wonder.ahk.compiler.parser;

import static fr.wonder.ahk.compiler.tokens.TokenBase.VAR_UNIT;

import java.util.ArrayList;

import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.utils.Assertions;

class AbstractParser {

	public static class ParsingException extends Exception {

		private static final long serialVersionUID = 1L;
		
	}

	public static class Pointer {
		
		public int position;
		
		public Pointer() {
			
		}
		
		public Pointer(int position) {
			this.position = position;
		}
		
	}
	
	static class ArgumentList extends ArrayList<FunctionArgument> {
		
		private static final long serialVersionUID = 9116932732646934043L;
	
		VarType[] getTypes() {
			return stream().map(arg -> arg.type).toArray(VarType[]::new);
		}
		
		String[] getNames() {
			return stream().map(arg -> arg.name).toArray(String[]::new);
		}
		
	}

	public static void assertHasNext(Token[] line, Pointer pointer, String error, ErrorWrapper errors) throws ParsingException {
		assertHasNext(line, pointer, error, errors, 1);
	}
	
	public static void assertHasNext(Token[] line, Pointer pointer, String error, ErrorWrapper errors, int count) throws ParsingException {
		Assertions.assertTrue(count > 0);
		if(line.length < pointer.position+count) {
			errors.add(error + ":" + line[line.length-1].getErr());
			throw new ParsingException();
		}
	}
	
	public static void assertNoRemainingTokens(Token[] line, Pointer pointer, ErrorWrapper errors) throws ParsingException {
		if(line.length != pointer.position) {
			errors.add("Unexpected tokens:" + line[pointer.position].getSource()
					.getErr(line, pointer.position, line.length));
			throw new ParsingException();
		}
	}

	/** Cannot parse composites or functions */
	public static VarType parseType(Unit unit, Token[] line, Pointer pointer, ErrorWrapper errors) {
		Token token = line[pointer.position];
		VarType baseType;
		if(token.base == VAR_UNIT)
			baseType = unit.getStructOrAliasType(token);
		else if(Tokens.isVarType(token.base))
			baseType = Tokens.typesMap.get(token.base);
		else {
			errors.add("Expected type:" + token.getErr());
			return Invalids.TYPE;
		}
		pointer.position++;
		while(pointer.position+1 < line.length &&
				line[pointer.position].base == TokenBase.TK_BRACKET_OPEN &&
				line[pointer.position+1].base == TokenBase.TK_BRACKET_CLOSE) {
			pointer.position += 2;
			baseType = new VarArrayType(baseType);
		}
		return baseType;
	}

	public static boolean expectToken(Token token, TokenBase base,
			String error, ErrorWrapper errors) {
		if(token.base != base) {
			errors.add(error + ':' + token.getErr());
			return false;
		}
		return true;
	}

	public static ArgumentList readArguments(Unit unit, Token[] line,
			Pointer pointer, boolean requireNames, ErrorWrapper errors) throws ParsingException {
		
		assertHasNext(line, pointer, "Incomplete composite", errors, 2);
		
		ArgumentList arguments = new ArgumentList();
		
		if(!expectToken(line[pointer.position], TokenBase.TK_PARENTHESIS_OPEN, "Expected '('", errors))
			throw new ParsingException();
		pointer.position++; // skip '('
		if(line[pointer.position].base == TokenBase.TK_PARENTHESIS_CLOSE) {
			pointer.position++; // skip ')'
			return arguments;
		}
		
		while(true) {
			assertHasNext(line, pointer, "Expected argument type", errors);
			int sourceStart = line[pointer.position].sourceStart;
			VarType type = parseType(unit, line, pointer, errors);
			String name = null;
			assertHasNext(line, pointer, "Incomplete composite", errors);
			Token nextTk = line[pointer.position++];
			if(nextTk.base == TokenBase.VAR_VARIABLE) {
				// skip optional argument name
				name = nextTk.text;
				assertHasNext(line, pointer, "Incomplete composite", errors);
				nextTk = line[pointer.position++];
			} else if(requireNames) {
				errors.add("Expected name:" + nextTk.getErr());
				name = "";
			}
			int sourceStop = nextTk.sourceStart;
			arguments.add(new FunctionArgument(unit.source, sourceStart, sourceStop, name, type));
			
			if(nextTk.base == TokenBase.TK_PARENTHESIS_CLOSE) {
				return arguments;
			} else if(!expectToken(nextTk, TokenBase.TK_COMMA, "Expected ','", errors)) {
				throw new ParsingException();
			}
		}
	}

	public static boolean assertParentheses(Token[] line, int begin, int end, ErrorWrapper errors) {
		boolean success = true;
		if(!expectToken(line[begin], TokenBase.TK_PARENTHESIS_OPEN, "Expected '('", errors))
			success = false;
		if(!expectToken(line[end], TokenBase.TK_PARENTHESIS_CLOSE, "Exppected ')'", errors))
			success = false;
		return success;
	}
}
