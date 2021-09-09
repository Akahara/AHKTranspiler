package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.Blueprint;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.GenericContext;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.UnimplementedException;

class BlueprintParser extends AbstractParser {

	public static Blueprint parseBlueprint(Unit unit, Token[][] lines, int begin, int end,
			DeclarationModifiers blueprintModifiers, ErrorWrapper errors) {
		
		List<VariableDeclaration> variables = new ArrayList<>();
		List<FunctionSection> functions = new ArrayList<>();
		
		Token[] declaration = lines[begin++];
		Blueprint blueprint;
		try {
			blueprint = parseBlueprintDeclaration(unit, declaration, blueprintModifiers, errors);
		} catch (ParsingException e) {
			return Invalids.BLUEPRINT;
		}
		
		ModifiersHolder modifiers = new ModifiersHolder();
		
		for(int i = begin; i < end; i++) {
			Token[] line = lines[i];
			
			if(line[0].base == TokenBase.VAR_MODIFIER) {
				modifiers.add(parseModifier(line, errors));
				
			} else if(Tokens.isDeclarationVisibility(line[0].base)) {
				modifiers.setVisibility(line[0], errors);
				
			} else if(Tokens.isVarType(line[0].base)) {
				DeclarationModifiers mods = modifiers.getModifiers();
				VariableDeclaration var = StatementParser.parseVariableDeclaration(
						unit, line, blueprint.genericContext, mods, errors);
				variables.add(var);
				
			} else if(line[0].base == TokenBase.KW_FUNC) {
				DeclarationModifiers mods = modifiers.getModifiers();
				if(line[line.length-1].base == TokenBase.TK_BRACE_OPEN) {
					errors.add("Blueprint functions cannot have a body:" + unit.source.getErr(line));
				}
				FunctionSection func = FunctionDeclarationParser.parseFunctionDeclaration(
						unit, line, line.length, blueprint.genericContext, mods, errors);
				functions.add(func);
				
			} else {
				errors.add("Unexpected line start:" + line[0].getErr());
			}
		}
		
		blueprint.variables = variables.toArray(VariableDeclaration[]::new);
		blueprint.functions = functions.toArray(FunctionSection[]::new);
		
		if(!variables.isEmpty())
			throw new UnimplementedException("Blueprints cannot have variables yet"); // TODO add variables to blueprints
		
		return blueprint;
	}
	
	private static Blueprint parseBlueprintDeclaration(Unit unit, Token[] declaration,
			DeclarationModifiers modifiers, ErrorWrapper errors) throws ParsingException {
		
		Pointer p = new Pointer(1);
		String name = assertToken(declaration, p, TokenBase.VAR_STRUCT, "Expected blueprint name", errors).text;
		GenericContext genericContext = readGenericArray(declaration, null, p, errors);
		assertToken(declaration, p, TokenBase.TK_BRACE_OPEN, "Expected '{' to begin blueprint", errors);
		
		return new Blueprint(unit, name, genericContext, modifiers, SourceReference.fromLine(declaration));
	}
	
}
