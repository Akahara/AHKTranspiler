package fr.wonder.ahk.compiler.parser;

import fr.wonder.ahk.compiled.expressions.types.VarFunctionType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.Alias;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.exceptions.ErrorWrapper;

class AliasDeclarationParser {
	
	static Alias parseAliasDeclaration(Unit unit, Token[] line, ErrorWrapper errors) {
		if(line.length < 4) {
			errors.add("Invalid alias declaration:" + unit.source.getErr(line));
			return Invalids.ALIAS;
		}
		if(!UnitParser.expectToken(line[1], TokenBase.VAR_UNIT, "alias name", errors) ||
			!UnitParser.expectToken(line[2], TokenBase.KW_EQUAL, "'='", errors))
			return Invalids.ALIAS;
		switch(line[3].base) {
		case KW_FUNC:
			VarFunctionType type = parseFunctionType(unit, line, 4, errors);
			return new Alias(unit.source, line[0].sourceStart,
					line[line.length-1].sourceStop, line[1].text, type);
		default:
			errors.add("Unexpected alias definition type:" + line[3].getErr());
			return Invalids.ALIAS;
		}
	}
	
	private static VarFunctionType parseFunctionType(Unit unit, Token[] line, int begin, ErrorWrapper errors) {
		if(line.length <= begin) {
			errors.add("Expected function type:" + unit.source.getErr(line[line.length-1].sourceStop));
			return Invalids.FUNCTION_TYPE;
		}
		VarType returnType;
		if(!Tokens.isVarType(line[begin].base)) {
			errors.add("Expected function return type:" + line[begin].getErr());
			returnType = Invalids.TYPE;
		} else {
			returnType = Tokens.getType(unit, line[begin]);
		}
		
		var args = FunctionDeclarationParser.readArguments(unit, line, begin+1, errors);
		if(args.b != line.length)
			errors.add("Unexpected tokens:" + unit.source.getErr(line, args.b, line.length));
		VarType[] arguments = args.a.stream().map(a->a.type).toArray(VarType[]::new);
		
		return new VarFunctionType(returnType, arguments);
	}
	
}
