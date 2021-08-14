package fr.wonder.ahk.compiler.parser;

import static fr.wonder.ahk.compiler.tokens.TokenBase.VAR_UNIT;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.utils.Assertions;

class AbstractParser {

	public static class ParsingException extends Exception {

		private static final long serialVersionUID = 1L;
		
		public ParsingException() {}
		public ParsingException(String e) { super(e); }
		
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

	static class ModifiersHolder {
		
		private final List<Modifier> modifiers = new ArrayList<>();
		private DeclarationVisibility visibility;
		
		DeclarationModifiers getModifiers() {
			if(visibility == null)
				visibility = DeclarationVisibility.LOCAL;
			DeclarationModifiers dm = new DeclarationModifiers(visibility, modifiers.toArray(Modifier[]::new));
			modifiers.clear();
			visibility = null;
			return dm;
		}
		
		public void add(Modifier modifier) {
			this.modifiers.add(modifier);
		}
	
		void setVisibility(Token tk, ErrorWrapper errors) {
			if(this.visibility != null)
				errors.add("Declaration visibility already specified:" + tk.getErr());
			this.visibility = Tokens.getDeclarationVisibility(tk.base);
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

	public static Token assertToken(Token[] line, Pointer p, TokenBase base,
			String error, ErrorWrapper errors) throws ParsingException {
		if(p.position == line.length) {
			errors.add(error + ':' + line[0].getSource().getErr(line[line.length-1].sourceStop));
			throw new ParsingException();
		}
		Token t = line[p.position];
		p.position++;
		if(t.base != base) {
			errors.add(error + ':' + t.getErr());
			throw new ParsingException();
		}
		return t;
	}
	
	/**
	 * Parses an argument list, may be used for function declarations or composite
	 * types.<br>
	 * (int, Struct, bool[][]) for example. Won't work with complex types that must
	 * be aliased (function types and composites). Names can be required, in which
	 * case no exception will be thrown if one is missing but an error will be reported
	 * in the error wrapper and the name will default to the empty string.
	 */
	public static ArgumentList readArguments(Unit unit, Token[] line,
			Pointer pointer, boolean requireNames, ErrorWrapper errors) throws ParsingException {
		
		return readArguments(() -> parseType(unit, line, pointer, errors),
				unit.source, line, pointer, requireNames, errors);
	}
	
	/**
	 * Used both by the simple
	 * {@link #readArguments(Unit, Token[], Pointer, boolean, ErrorWrapper)} method
	 * and by the alias parser, in which case the {@code typeParser} argument will
	 * be {@link AliasDeclarationParser#parseType(Unit, Token[], Pointer, ErrorWrapper)}
	 * which can parse complex types that should be aliased otherwise.
	 */
	public static ArgumentList readArguments(TypeParser typeParser, UnitSource source, Token[] line,
			Pointer pointer, boolean requireNames, ErrorWrapper errors) throws ParsingException {

		assertHasNext(line, pointer, "Incomplete composite", errors, 2);
		
		ArgumentList arguments = new ArgumentList();
		
		assertToken(line, pointer, TokenBase.TK_PARENTHESIS_OPEN, "Expected '('", errors);
		if(line[pointer.position].base == TokenBase.TK_PARENTHESIS_CLOSE) {
			pointer.position++; // skip ')'
			return arguments;
		}
		
		while(true) {
			assertHasNext(line, pointer, "Expected argument type", errors);
			int sourceStart = line[pointer.position].sourceStart;
			VarType type = typeParser.parseType();
			String name = "";
			assertHasNext(line, pointer, "Incomplete composite", errors);
			Token nextTk = line[pointer.position++];
			if(nextTk.base == TokenBase.VAR_VARIABLE) {
				// skip optional argument name
				name = nextTk.text;
				assertHasNext(line, pointer, "Incomplete composite", errors);
				nextTk = line[pointer.position++];
			} else if(requireNames) {
				errors.add("Expected name:" + nextTk.getErr());
			}
			int sourceStop = nextTk.sourceStart;
			arguments.add(new FunctionArgument(source, sourceStart, sourceStop, name, type));
			
			if(nextTk.base == TokenBase.TK_PARENTHESIS_CLOSE)
				return arguments;
			else if(nextTk.base != TokenBase.TK_COMMA)
				errors.add("Expected ',':" + nextTk.getErr());
		}
	}
	
	protected static interface TypeParser {
		
		/** Will use the pointer, error wrapper and token line given to #readArguments */
		public VarType parseType() throws ParsingException;
		
	}

	public static void assertParentheses(Token[] line, int begin, int end, ErrorWrapper errors) throws ParsingException {
		assertToken(line, new Pointer(begin), TokenBase.TK_PARENTHESIS_OPEN, "Expected '('", errors);
		assertToken(line, new Pointer(end), TokenBase.TK_PARENTHESIS_CLOSE, "Exppected ')'", errors);
	}

	/** Assumes that the first line token base is VAR_MODIFIER */
	static Modifier parseModifier(Token[] line, ErrorWrapper errors) {
		if(line.length == 1)
			return new Modifier(line[0].text.substring(1));
		
		if(line.length < 3) {
			errors.add("Invalid modifier syntax:" + line[0].getErr());
		} else if(line[1].base != TokenBase.TK_PARENTHESIS_OPEN || line[line.length-1].base != TokenBase.TK_PARENTHESIS_CLOSE) {
			Token t = line[1].base != TokenBase.TK_PARENTHESIS_OPEN ? line[1] : line[line.length-1];
			errors.add("Invalid modifier syntax:" + t.getErr());
			return null;
		}
		LiteralExp<?>[] arguments = new LiteralExp[(line.length-2)/2];
		ErrorWrapper subErrors = errors.subErrrors("Unable to parse modifier parameter, expected literal");
		for(int i = 2; i < line.length-1; i++) {
			if(i%2 == 0) {
				if(line[i].base == TokenBase.LIT_NULL)
					errors.add("Invalid modifier value, null values are not accepted:" + line[i].getErr());
				else if(!Tokens.isLiteral(line[i].base))
					errors.add("Invalid modifier syntax, expected argument:" + line[i].getErr());
				else
					arguments[(i-2)/2] = ExpressionParser.parseLiteral(line[i], subErrors);
				
			} else {
				if(line[i].base != TokenBase.TK_COMMA)
					errors.add("Invalid modifier syntax:" + line[i].getErr());
			}
		}
		return new Modifier(line[0].text.substring(1), arguments);
	}
}
