package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarCompositeType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.types.Tuple;

class FunctionDeclarationParser {

	/** Assumes that the first line token is KW_FUNC and the last TK_BRACE_OPEN */
	static FunctionSection parseFunctionSection(Unit unit, Token[][] lines,
			int start, int stop, DeclarationModifiers modifiers, ErrorWrapper errors) {
		
		Token[] declaration = lines[start];
		FunctionSection function = new FunctionSection(
				unit.source,
				declaration[0].sourceStart, // source start
				lines[stop-1][lines[stop-1].length-1].sourceStop, // source stop
				declaration[declaration.length-1].sourceStop, // declaration stop
				modifiers);
		
		readFunctionDeclaration(unit, function, declaration, errors.subErrrors("Invalid function declaration"));
		
		function.body = new Statement[stop-start-1];
		ErrorWrapper functionErrors = errors.subErrrors("Unable to parse function");
		for(int i = start+1; i < stop; i++)
			function.body[i-start-1] = StatementParser.parseStatement(unit, lines[i], functionErrors);
		if(functionErrors.noErrors() && !modifiers.hasModifier(Modifier.NATIVE))
			StatementsFinalizer.finalizeStatements(function);
		return function;
	}

	private static void readFunctionDeclaration(Unit unit, FunctionSection func, Token[] declaration, ErrorWrapper errors) {
		if(declaration.length < 6) {
			errors.add("Incomplete function declaration" + func.getErr());
			return;
		}
		
		int k;
		if(declaration[1].base == TokenBase.TK_PARENTHESIS_OPEN) {
			Tuple<List<Argument>, Integer> composite = readArguments(unit, declaration, 1, errors);
			k = composite.b;
			String[] names = new String[composite.a.size()];
			VarType[] types = new VarType[composite.a.size()];
			for(int i = 0; i < composite.a.size(); i++) {
				Argument arg = composite.a.get(i);
				names[i] = arg.name;
				types[i] = arg.type;
			}
			func.returnType = new VarCompositeType(names, types);
		} else if(declaration[1].base == TokenBase.TYPE_VOID) {
			func.returnType = VarType.VOID;
			k = 2;
		} else {
			if(!Tokens.isVarType(declaration[1].base))
				errors.add("Expected return type" + declaration[1].getErr());
			func.returnType = Tokens.getType(unit, declaration[1]);
			k = 2;
		}
		
		if(declaration.length - k < 3) {
			errors.add("Incomplete function declaration" + func.getErr());
			func.name = Invalids.STRING;
			func.arguments = new FunctionArgument[0];
			return;
		}
		
		if(declaration[k].base != TokenBase.VAR_VARIABLE)
			errors.add("Expected function name:" + declaration[k].base);
		func.name = declaration[k].text;
		
		k++;
		
		Tuple<List<Argument>, Integer> composite = readArguments(unit, declaration, k, errors);
		List<Argument> arguments = composite.a;
		k = composite.b;
		
		func.arguments = new FunctionArgument[arguments.size()];
		for(int i = 0; i < arguments.size(); i++) {
			Argument arg = arguments.get(i);
			func.arguments[i] = arg.asFunctionArgument(unit.source);
		}
		
		if(k != declaration.length-1)
			errors.add("Unexpected tokens" + unit.source.getErr(declaration, k, declaration.length-1));
	}
	
	static class Argument {
		
		String name;
		VarType type;
		int sourceStart, sourceStop;
		
		FunctionArgument asFunctionArgument(UnitSource source) {
			return new FunctionArgument(source, sourceStart, sourceStop, name, type);
		}
		
	}
	
	static Tuple<List<Argument>, Integer> readArguments(
			Unit unit, Token[] tokens, int begin, ErrorWrapper errors) {
		
		if(tokens[begin].base != TokenBase.TK_PARENTHESIS_OPEN) {
			errors.add("Expected '(' :" + tokens[begin].getErr());
			return new Tuple<>(Collections.emptyList(), begin);
		} else if(tokens[begin+1].base == TokenBase.TK_PARENTHESIS_CLOSE) {
			return new Tuple<>(Collections.emptyList(), begin+2);
		}
		
		List<Argument> arguments = new ArrayList<>();
		int k = begin+1;
		while(true) {
			if(tokens.length < k+3) {
				errors.add("Incomplete composite:" + tokens[tokens.length-1].getErr());
				return new Tuple<>(arguments, tokens.length-1);
			}
			Argument arg = new Argument();
			Token type = tokens[k];
			Token name = tokens[k+1];
			if(!Tokens.isVarType(type.base))
				errors.add("Expected type" + type.getErr());
			arg.type = Tokens.getType(unit, type);
			arg.sourceStart = type.sourceStart;
			if(name.base != TokenBase.VAR_VARIABLE)
				errors.add("Expected name" + name.getErr());
			arg.name = name.text;
			arg.sourceStop = name.sourceStop;
			arguments.add(arg);
			if(tokens[k+2].base == TokenBase.TK_PARENTHESIS_CLOSE) {
				return new Tuple<>(arguments, k+3);
			} else if(tokens[k+2].base != TokenBase.TK_COMMA) {
				errors.add("Expected ',' :" + tokens[k+2].getErr());
				return new Tuple<>(arguments, k+1);
			}
			k += 3;
		}
	}

}
