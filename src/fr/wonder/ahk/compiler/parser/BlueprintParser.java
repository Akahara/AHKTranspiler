package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.Blueprint;
import fr.wonder.ahk.compiled.units.sections.BlueprintOperator;
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
		List<BlueprintOperator> operators = new ArrayList<>();
		
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
						unit, line, GenericContext.NO_CONTEXT, mods, errors);
				variables.add(var);
				
			} else if(line[0].base == TokenBase.KW_FUNC) {
				DeclarationModifiers mods = modifiers.getModifiers();
				if(line[line.length-1].base == TokenBase.TK_BRACE_OPEN) {
					errors.add("Blueprint functions cannot have a body:" + unit.source.getErr(line));
				}
				FunctionSection func = FunctionDeclarationParser.parseFunctionDeclaration(
						unit, line, line.length, GenericContext.NO_CONTEXT, mods, errors);
				functions.add(func);
				
			} else if(line[0].base == TokenBase.KW_OPERATOR) {
				DeclarationModifiers mods = modifiers.getModifiers();
				try {
					operators.add(parseOperator(blueprint, line, mods, errors));
				} catch (ParsingException x) {}
				
			} else {
				errors.add("Unexpected line start:" + line[0].getErr());
			}
		}
		
		blueprint.variables = variables.toArray(VariableDeclaration[]::new);
		blueprint.functions = functions.toArray(FunctionSection[]::new);
		blueprint.operators = operators.toArray(BlueprintOperator[]::new);
		
		if(!variables.isEmpty())
			throw new UnimplementedException("Blueprints cannot have variables yet"); // TODO add variables to blueprints
		
		return blueprint;
	}
	
	private static Blueprint parseBlueprintDeclaration(Unit unit, Token[] declaration,
			DeclarationModifiers modifiers, ErrorWrapper errors) throws ParsingException {
		
		Pointer p = new Pointer(1);
		String name = assertToken(declaration, p, TokenBase.VAR_BLUEPRINT, "Expected blueprint name", errors).text;
		assertToken(declaration, p, TokenBase.TK_BRACE_OPEN, "Expected '{' to begin blueprint", errors);
		return new Blueprint(unit, name, modifiers, SourceReference.fromLine(declaration));
	}
	
	/** assumes that line[1].base is KW_OPERATOR */
	private static BlueprintOperator parseOperator(Blueprint blueprint, Token[] line,
			DeclarationModifiers modifiers, ErrorWrapper errors) throws ParsingException {
		
		Pointer p = new Pointer(1);
		VarType lo = parseType(blueprint.unit, line, GenericContext.NO_CONTEXT, p, ALLOW_SELF, errors);
		assertHasNext(line, p, "Unfinished operator definition", errors);
		Token operatorToken = line[p.position++];
		Operator operator;
		if(!Tokens.isOperator(operatorToken.base)) {
			errors.add("Expected operator:" + operatorToken.getErr());
			throw new ParsingException();
		} else {
			operator = Tokens.getOperator(operatorToken.base);
		}
		VarType ro = parseType(blueprint.unit, line, GenericContext.NO_CONTEXT, p, ALLOW_SELF, errors);
		assertToken(line, p, TokenBase.KW_EQUAL, "Expected '=' in operator definition", errors);
		VarType resultType = parseType(blueprint.unit, line, GenericContext.NO_CONTEXT, p, ALLOW_SELF, errors);
		return new BlueprintOperator(SourceReference.fromLine(line), lo, ro, operator, resultType);
	}
	
}
