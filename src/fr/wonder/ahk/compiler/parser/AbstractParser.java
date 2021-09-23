package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarBoundStructType;
import fr.wonder.ahk.compiled.expressions.types.VarGenericType;
import fr.wonder.ahk.compiled.expressions.types.VarSelfType;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.BlueprintRef;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.GenericContext;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.utils.Assertions;

public class AbstractParser {

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
		
		FunctionArgument[] asArray() {
			return toArray(FunctionArgument[]::new);
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
			errors.add("Unexpected tokens:" + line[pointer.position].sourceRef.source
					.getErr(line, pointer.position, line.length));
			throw new ParsingException();
		}
	}
	
	public static final int ALLOW_NONE = 0, ALLOW_SELF = 1;
	
	/** Cannot parse composites or functions */
	public static VarType parseType(Unit unit, Token[] line, GenericContext genc, Pointer pointer, int allowedFields, ErrorWrapper errors) {
		
		if(pointer.position >= line.length) {
			errors.add("Expected type:" + unit.source.getErr(line[line.length-1].getSourceReference().stop));
			return Invalids.TYPE;
		}
		
		Token token = line[pointer.position];
		VarType baseType;
		if(token.base == TokenBase.VAR_STRUCT) {
			baseType = unit.getStructOrAliasType(token);
		} else if(token.base == TokenBase.VAR_GENERIC) {
			baseType = genc.getGenericType(token, errors);
		} else if(token.base == TokenBase.KW_SELF) {
			if((allowedFields & ALLOW_SELF) != 0) {
				baseType = VarSelfType.SELF;
			} else {
				errors.add("Self is not acceptable in this context:" + token.getErr());
				return Invalids.TYPE;
			}
		} else if(Tokens.isVarType(token.base)) {
			baseType = Tokens.typesMap.get(token.base);
		} else {
			errors.add("Expected type:" + token.getErr());
			return Invalids.TYPE;
		}
		pointer.position++;
		
		if(pointer.position < line.length && line[pointer.position].base == TokenBase.TK_GENERIC_BINDING_BEGIN) {
			pointer.position++;
			List<VarType> typeParameters = new ArrayList<>();
			while(true) {
				typeParameters.add(parseType(unit, line, genc, pointer, allowedFields, errors));
				if(pointer.position == line.length) {
					errors.add("Unfinished type parameters:" + line[line.length-1].getErr());
					return Invalids.TYPE;
				}
				Token nextTk = line[pointer.position];
				pointer.position++;
				if(nextTk.base == TokenBase.TK_GENERIC_BINDING_END)
					break;
				if(nextTk.base != TokenBase.TK_COMMA)
					errors.add("Expected ',' in type parameters:" + nextTk.getErr());
			}
			if(typeParameters.isEmpty())
				errors.add("Cannot make an empty parametrized type:" + unit.source.getErr(line, pointer.position-2, pointer.position));
			else if(!(baseType instanceof VarStructType))
				errors.add("Type " + baseType + " cannot be parametrized:" + line[pointer.position-1].getErr());
			else {
				VarBoundStructType bound = new VarBoundStructType(baseType.getName(), typeParameters.toArray(VarType[]::new));
				unit.usedStructTypes.addParametrizedInstance(bound, genc, token);
				baseType = bound;
			}
		}
		
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
			errors.add(error + ':' + line[0].sourceRef.source.getErr(line[line.length-1].sourceRef.stop));
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
			boolean requireNames, GenericContext genc, Pointer pointer,
			int allowedFields, ErrorWrapper errors) throws ParsingException {
		
		return readArguments(() -> parseType(unit, line, genc, pointer, allowedFields, errors),
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
			int sourceStart = line[pointer.position].sourceRef.start;
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
			int sourceStop = nextTk.sourceRef.start;
			SourceReference sourceRef = new SourceReference(source, sourceStart, sourceStop);
			arguments.add(new FunctionArgument(sourceRef, name, type));
			
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
		ErrorWrapper subErrors = errors.subErrors("Unable to parse modifier parameter, expected literal");
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
	
	public static GenericContext readGenericArray(Unit unit, Token[] line,
			GenericContext parentContext, Pointer p, ErrorWrapper errors) throws ParsingException {
		
		if(p.position == line.length)
			return new GenericContext(parentContext, GenericContext.NO_GENERICS);
		if(line[p.position].base != TokenBase.TK_GENERIC_BINDING_BEGIN)
			return new GenericContext(parentContext, GenericContext.NO_GENERICS);
		p.position++;
		
		Map<String, VarGenericType> generics = new LinkedHashMap<>();
		
		assertHasNext(line, p, "Expected generic array", errors);
		if(line[p.position].base == TokenBase.TK_GENERIC_BINDING_END) {
			errors.add("Generic bindings cannot be empty:" +
					SourceReference.fromLine(line, p.position-1, p.position).getErr());
			return Invalids.GENERIC_CONTEXT;
		}
		
		while(true) {
			assertHasNext(line, p, "Unfinished generic array", errors);
			Token tk = line[p.position++];
			if(tk.base != TokenBase.VAR_GENERIC) {
				errors.add("Expected generic:" + tk.getErr());
				continue;
			}
			
			String name = tk.text;
			BlueprintRef[] typeRestrictions;
			
			if((parentContext != null && parentContext.retrieveGenericType(name) != null) ||
					generics.containsKey(name)) {
				errors.add("Cannot use already bound generic '" + name + "':" + tk.getErr());
			}
			
			assertHasNext(line, p, "Unfinished generic array", errors);
			tk = line[p.position++];
			
			if(tk.base == TokenBase.TK_COLUMN) {
				typeRestrictions = readGenericRestriction(unit, line, p, errors);
				assertHasNext(line, p, "Unfinished generic array", errors);
				tk = line[p.position++];
			} else {
				typeRestrictions = VarGenericType.NO_TYPE_RESTRICTION;
			}

			generics.put(name, new VarGenericType(name, typeRestrictions));
			
			if(tk.base == TokenBase.TK_GENERIC_BINDING_END) {
				return new GenericContext(parentContext, generics.values().toArray(VarGenericType[]::new));
			} else if(tk.base != TokenBase.TK_COMMA) {
				errors.add("Expected ',' in generic array:" + tk.getErr());
			}
		}
	}

	public static BlueprintRef[] readGenericRestriction(Unit unit, Token[] line, Pointer p, ErrorWrapper errors) throws ParsingException {
		List<BlueprintRef> restrictions = new ArrayList<>();
		while(true) {
			Token blueprint = assertToken(line, p, TokenBase.VAR_BLUEPRINT, "Expected blueprint name in generic restrictions", errors);
			restrictions.add(unit.usedBlueprintTypes.getType(blueprint));
			assertHasNext(line, p, "Unfinished generic restriction", errors);
			Token tk = line[p.position];
			if(tk.base == TokenBase.OP_AND) {
				p.position++;
				continue;
			} else {
				return restrictions.toArray(BlueprintRef[]::new);
			}
		}
	}
}
