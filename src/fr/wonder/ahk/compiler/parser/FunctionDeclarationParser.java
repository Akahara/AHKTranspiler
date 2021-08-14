package fr.wonder.ahk.compiler.parser;

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
import fr.wonder.commons.exceptions.ErrorWrapper;

class FunctionDeclarationParser extends AbstractParser {

	/** Assumes that the first line token is KW_FUNC and the last TK_BRACE_OPEN */
	static FunctionSection parseFunctionSection(Unit unit, Token[][] lines,
			int start, int stop, DeclarationModifiers modifiers, ErrorWrapper errors) {
		
		Token[] declaration = lines[start];
		FunctionSection function = new FunctionSection(
				unit,
				declaration[0].sourceStart, // source start
				lines[stop-1][lines[stop-1].length-1].sourceStop, // source stop
				declaration[declaration.length-1].sourceStop, // declaration stop
				modifiers);
		
		try {
			ErrorWrapper subErrors = errors.subErrrors("Invalid function declaration");
			readFunctionDeclaration(unit, function, declaration, subErrors);
		} catch (ParsingException e) {
			return Invalids.FUNCTION;
		}
		
		function.body = new Statement[stop-start-1];
		ErrorWrapper functionErrors = errors.subErrrors("Unable to parse function");
		for(int i = start+1; i < stop; i++)
			function.body[i-start-1] = StatementParser.parseStatement(unit, lines[i], functionErrors);
		if(functionErrors.noErrors() && !modifiers.hasModifier(Modifier.NATIVE))
			StatementsFinalizer.finalizeStatements(function);
		return function;
	}

	private static void readFunctionDeclaration(Unit unit, FunctionSection func, Token[] declaration, ErrorWrapper errors) throws ParsingException {
		if(declaration.length < 6) {
			errors.add("Incomplete function declaration" + func.getErr());
			return;
		}
		
		Pointer pointer = new Pointer(1);
		
		if(declaration[pointer.position].base == TokenBase.TK_PARENTHESIS_OPEN) {
			ArgumentList args = readArguments(unit, declaration, pointer, true, errors);
			func.returnType = new VarCompositeType(args.getNames(), args.getTypes());
		} else if(declaration[pointer.position].base == TokenBase.TYPE_VOID) {
			func.returnType = VarType.VOID;
			pointer.position++;
		} else {
			func.returnType = parseType(unit, declaration, pointer, errors);
		}
		
		assertHasNext(declaration, pointer, "Incomplete function declaration", errors, 3);
		
		func.name = assertToken(declaration, pointer, TokenBase.VAR_VARIABLE, "Expected function name", errors).text;
		
		ArgumentList arguments = readArguments(unit, declaration, pointer, true, errors);
		
		func.arguments = arguments.toArray(FunctionArgument[]::new);

		if(pointer.position != declaration.length-1)
			errors.add("Unexpected tokens" + unit.source.getErr(declaration,
					pointer.position, declaration.length-1));
	}

}
