package fr.wonder.ahk.compiler.parser;

import fr.wonder.ahk.compiled.expressions.types.VarCompositeType;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.units.SourceReference;
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
	
	/**
	 * Parses a function declaration but not its body.
	 * 
	 * <p>
	 * The function {@link FunctionSection#body} is left null,
	 * {@link #parseFunctionBody(FunctionSection, Token[][], int, int, ErrorWrapper)}
	 * must be used to read it.
	 * 
	 * <p>
	 * Assumes that the declaration line starts with TK_FUNC.
	 * 
	 * @param declarationLength the length of the function declaration (in token
	 *                          count), if the declaration ends with '{' the length
	 *                          must be 1 less than the line length.
	 */
	static FunctionSection parseFunctionDeclaration(Unit unit, Token[] declaration,
			int declarationLength, DeclarationModifiers modifiers, ErrorWrapper errors) {
		
		try {
			SourceReference funcSourceRef = SourceReference.fromLine(declaration);
			
			if(declaration.length < 6) {
				errors.add("Incomplete function declaration" + funcSourceRef.getErr());
				return Invalids.FUNCTION;
			}
			
			Pointer pointer = new Pointer(1);
			
			VarType returnType;
			String funcName;
			FunctionArgument[] funcArgs;
			
			if(declaration[pointer.position].base == TokenBase.TK_PARENTHESIS_OPEN) {
				ArgumentList args = readArguments(unit, declaration, true, pointer, errors);
				returnType = new VarCompositeType(args.getNames(), args.getTypes());
			} else if(declaration[pointer.position].base == TokenBase.TYPE_VOID) {
				returnType = VarType.VOID;
				pointer.position++;
			} else {
				returnType = parseType(unit, declaration, pointer, errors);
			}
			
			assertHasNext(declaration, pointer, "Incomplete function declaration", errors, 3);
			
			funcName = assertToken(declaration, pointer, TokenBase.VAR_VARIABLE, "Expected function name", errors).text;
			
			funcArgs = readArguments(unit, declaration, true, pointer, errors).asArray();
			
			if(pointer.position != declarationLength)
				errors.add("Unexpected tokens" + unit.source.getErr(declaration,
						pointer.position, declaration.length-1));
			
			FunctionSection function = new FunctionSection(unit, funcSourceRef, modifiers);
			
			function.arguments = funcArgs;
			function.name = funcName;
			function.returnType = returnType;
			
			return function;
		} catch (ParsingException e) {
			return Invalids.FUNCTION;
		}
	}
	
	/**
	 * Sets the body of the given function by parsing its statements
	 * 
	 * @param start the first statement line index, <b>not the declartion line</b>
	 * @param stop the index of the line containing only '}', the function's end
	 */
	static FunctionSection parseFunctionBody(FunctionSection function, Token[][] lines,
			int start, int stop, ErrorWrapper errors) {
		
		function.body = new Statement[stop-start];
		ErrorWrapper functionErrors = errors.subErrors("Unable to parse function");
		for(int i = start; i < stop; i++)
			function.body[i-start] = StatementParser.parseStatement(function.unit, lines[i], functionErrors);
		if(functionErrors.noErrors() && !function.modifiers.hasModifier(Modifier.NATIVE))
			StatementsFinalizer.finalizeStatements(function);
		return function;
	}
	
}
