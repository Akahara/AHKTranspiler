package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.types.VarArrayType;
import fr.wonder.ahk.compiled.expressions.types.VarCompositeType;
import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.Alias;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;
import fr.wonder.commons.exceptions.UnreachableException;
import fr.wonder.commons.types.Tuple;
import fr.wonder.commons.utils.Assertions;

public class AliasDeclarationParser extends AbstractParser {
	
	private static final byte 
			STATUS_NOT_PARSED = 0,
			STATUS_PARSING = 1,
			STATUS_RESOLVED = 2;
	
	public static void resolveAliases(Unit[] units, Token[][][] unitsTokens, ErrorWrapper errors) throws WrappedException {
		UnitAliases[] unitAliases = new UnitAliases[units.length];
		
		for(int i = 0; i < units.length; i++) {
			Unit unit = units[i];
			ErrorWrapper unitErrors = errors.subErrrors("Cannot resolve alias declarations of unit " + unit.fullBase);
			
			UnitAliases unitAlias = new UnitAliases(unit, unitErrors);
			unitAliases[i] = unitAlias;
			
			collectAliasLines(unitAlias, unitsTokens[i]);
		}
		
		for(UnitAliases parser : unitAliases) {
			parser.accessibleUnits[0] = parser;
			for(int i = 1; i < parser.accessibleUnits.length; i++)
				parser.accessibleUnits[i] = getParser(unitAliases, parser.unit.importations[i-1]);
		}
		
		errors.assertNoErrors();
		
		for(UnitAliases unit : unitAliases) {
			int accessibleAliasCount = 0;
			for(UnitAliases accessible : unit.accessibleUnits)
				accessibleAliasCount += accessible.aliases.size();
			unit.unit.accessibleAliases = new Alias[accessibleAliasCount];
			
			for(int i = 0; i < unit.aliases.size(); i++) {
				AliasParser alias = unit.aliases.get(i);
				unit.unit.accessibleAliases[i] = resolveAlias(alias);
			}
		}
		
		for(UnitAliases unit : unitAliases) {
			int aidx = unit.unit.declaredAliasCount;
			for(int i = 1; i < unit.accessibleUnits.length; i++) {
				UnitAliases accessible = unit.accessibleUnits[i];
				for(int j = 0; j < accessible.aliases.size(); j++) {
					unit.unit.accessibleAliases[aidx++] = accessible.aliases.get(j).resolvedAlias;
				}
			}
		}
	}
	
	private static void collectAliasLines(UnitAliases unitAliases, Token[][] unitLines) {
		Unit unit = unitAliases.unit;
		int firstAliasLine = unit.importations.length + 2;
		
		for(int j = 0; j < unit.declaredAliasCount; j++) {
			Token[] line = unitLines[firstAliasLine+j];
			
			Assertions.assertTrue(line[0].base == TokenBase.KW_ALIAS);
			
			if(line.length < 3) {
				unitAliases.errors.add("Invalid alias declaration:" + unit.source.getErr(line));
			} else if(AbstractParser.expectToken(line[1], TokenBase.VAR_UNIT, "alias name", unitAliases.errors) &&
				AbstractParser.expectToken(line[2], TokenBase.KW_EQUAL, "'='", unitAliases.errors)) {
				
				String aliasName = line[1].text;
				ErrorWrapper aliasErrors = unitAliases.errors.subErrrors("Cannot resolve alias " + aliasName);
				unitAliases.aliases.add(new AliasParser(unitAliases, line, aliasName, aliasErrors));
			}
		}
	}
	
	private static UnitAliases getParser(UnitAliases[] unitAliases, String unitFullBase) {
		for(UnitAliases parser : unitAliases) {
			if(parser.unit.fullBase.equals(unitFullBase))
				return parser;
		}
		throw new UnreachableException("Unit dependencies were not checked");
	}
	
	private static class UnitAliases {
		
		final Unit unit;
		final List<AliasParser> aliases = new ArrayList<>();
		final ErrorWrapper errors;
		final UnitAliases[] accessibleUnits;
		
		UnitAliases(Unit unit, ErrorWrapper errors) {
			this.unit = unit;
			this.errors = errors;
			this.accessibleUnits = new UnitAliases[1+unit.importations.length];
		}
		
	}
	
	private static class AliasParser {
		
		UnitAliases unit;
		Token[] line;
		String name;
		byte parsingStatus;
		ErrorWrapper errors;
		
		Alias resolvedAlias;
		
		AliasParser(UnitAliases unit, Token[] line, String name, ErrorWrapper errors) {
			this.unit = unit;
			this.line = line;
			this.parsingStatus = STATUS_NOT_PARSED;
			this.name = name;
			this.errors = errors;
		}
		
	}
	
	private static Alias resolveAlias(AliasParser alias) {
		if(alias.parsingStatus == STATUS_PARSING) {
			alias.errors.add("Cyclic definition"); // TODO find a way to print the cycle
			return Invalids.ALIAS;
		} else if(alias.parsingStatus == STATUS_RESOLVED) {
			return alias.resolvedAlias;
		}
		
		alias.parsingStatus = STATUS_PARSING;
		
		try {
			Pointer pointer = new Pointer(3);
			VarType type = parseType(alias, pointer);
			
			if(pointer.position != alias.line.length) {
				alias.errors.add("Unexpected tokens:" 
						+ alias.unit.unit.source.getErr(alias.line, pointer.position, alias.line.length));
				throw new ParsingException();
			}
			
			alias.resolvedAlias = new Alias(
					alias.unit.unit.source,
					alias.line[0].sourceStart,
					alias.line[alias.line.length-1].sourceStop,
					alias.name, type);
		} catch (ParsingException e) {
			alias.resolvedAlias = Invalids.ALIAS;
		}
		
		alias.parsingStatus = STATUS_RESOLVED;
		
		return alias.resolvedAlias;
	}
	
	private static VarType parseType(AliasParser alias, Pointer pointer) throws ParsingException {
		ErrorWrapper errors = alias.errors;
		Token[] line = alias.line;
		
		if(pointer.position == line.length) {
			errors.add("Invalid alias declaration:" + alias.unit.unit.source.getErr(line));
			throw new ParsingException();
		}
		
		VarType baseType;
		Token firstToken = line[pointer.position];
		
		if(Tokens.typesMap.containsKey(firstToken.base)) {
			baseType = Tokens.typesMap.get(firstToken.base);
			pointer.position++;
		} else if(firstToken.base == TokenBase.KW_FUNC) {
			baseType = parseFunctionType(alias, pointer);
		} else if(firstToken.base == TokenBase.VAR_UNIT) {
			baseType = resolveStructOrAlias(alias, firstToken);
			pointer.position++;
		} else if(firstToken.base == TokenBase.TK_PARENTHESIS_OPEN) {
			baseType = parseCompositeType(alias, pointer);
		} else {
			errors.add("Unexpected alias definition type:" + firstToken.getErr());
			throw new ParsingException();
		}
		
		while(pointer.position+1 < line.length &&
				line[pointer.position].base == TokenBase.TK_BRACKET_OPEN &&
				line[pointer.position+1].base == TokenBase.TK_BRACKET_CLOSE) {
			
			pointer.position += 2;
			baseType = new VarArrayType(baseType);
		}
		
		return baseType;
	}
	
	private static VarType resolveStructOrAlias(AliasParser alias, Token token) {
		for(UnitAliases unit : alias.unit.accessibleUnits) {
			for(AliasParser a : unit.aliases) {
				if(a.name.equals(token.text))
					return resolveAlias(a).resolvedType;
			}
		}
		return alias.unit.unit.getStructType(token);
	}
	
	/** assumes that line[pointer] is a KW_FUNC */
	private static VarType parseFunctionType(AliasParser alias, Pointer pointer) throws ParsingException {
		assertHasNext(alias, pointer, 3);
		
		pointer.position++; // skip the 'func' keyword
		VarType returnType = parseType(alias, pointer);
		
		VarType[] arguments = readArguments(alias, pointer, false).a;
		
		return new VarFunctionType(returnType, arguments);
	}
	
	private static VarType parseCompositeType(AliasParser alias, Pointer pointer) throws ParsingException {
		var args = readArguments(alias, pointer, true);
		return new VarCompositeType(args.b, args.a);
	}
	
	private static Tuple<VarType[], String[]> readArguments(AliasParser alias, Pointer pointer, boolean requireNames) throws ParsingException {
		Token[] line = alias.line;
		assertHasNext(alias, pointer, 2);
		
		if(!AbstractParser.expectToken(line[pointer.position], TokenBase.TK_PARENTHESIS_OPEN, "'('", alias.errors))
			throw new ParsingException();
		pointer.position++; // skip '('
		if(line[pointer.position].base == TokenBase.TK_PARENTHESIS_CLOSE) {
			pointer.position++; // skip ')'
			return new Tuple<>(new VarType[0], new String[0]);
		}
		
		List<VarType> arguments = new ArrayList<>();
		List<String> names = new ArrayList<>();
		while(true) {
			assertHasNext(alias, pointer);
			VarType type = parseType(alias, pointer);
			arguments.add(type);
			assertHasNext(alias, pointer);
			Token nextTk = line[pointer.position++];
			if(nextTk.base == TokenBase.VAR_VARIABLE) {
				// skip optional argument name
				names.add(nextTk.text);
				assertHasNext(alias, pointer);
				nextTk = line[pointer.position++];
			} else if(requireNames) {
				alias.errors.add("Expected name:" + nextTk.getErr());
			}
			if(nextTk.base == TokenBase.TK_PARENTHESIS_CLOSE) {
				VarType[] argsArr = arguments.toArray(VarType[]::new);
				String[] namesArr = names.toArray(String[]::new);
				return new Tuple<>(argsArr, namesArr);
			} else if(!AbstractParser.expectToken(nextTk, TokenBase.TK_COMMA, ",", alias.errors)) {
				throw new ParsingException();
			}
		}
	}
	
	private static void assertHasNext(AliasParser alias, Pointer pointer) throws ParsingException {
		assertHasNext(alias, pointer, 1);
	}
	
	private static void assertHasNext(AliasParser alias, Pointer pointer, int count) throws ParsingException {
		assertHasNext(alias.line, pointer, "Unexpected alias end", alias.errors);
	}
	
}
