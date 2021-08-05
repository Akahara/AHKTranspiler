package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.UnitCompilationState;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public class UnitParser extends AbstractParser {
	
	public static void parseUnit(Unit unit, Token[][] tokens, ErrorWrapper errors) throws WrappedException {
		unit.compilationState = UnitCompilationState.PARSED_WITH_ERRORS;
		
		int declarationEnd = unit.importations.length + unit.declaredAliasCount + 2;
		parseSections(unit, tokens, declarationEnd, errors.subErrrors("Unable to parse unit body"));
		errors.assertNoErrors();
		
		unit.compilationState = UnitCompilationState.PARSED;
	}
	
	public static Unit preparseUnit(UnitSource source, Token[][] lines, ErrorWrapper errors) throws WrappedException {
		String base = null;
		String[] importations;
		String name = null;
		
		if(lines.length < 2) {
			errors.add("Incomplete ahk file:" + source.getErr(0));
			errors.assertNoErrors();
		}
		
		// parse unit base
		
		Token[] baseLine = lines[0];
		
		if(baseLine.length < 2 || baseLine[0].base != TokenBase.DECL_BASE) {
			errors.add("Missing header declaration!" + baseLine[0].getErr());
		} else {
			base = "";
			for(int i = 1; i < baseLine.length; i++) {
				if((i%2 == 0 && baseLine[i].base != TokenBase.TK_DOT) ||
					(i%2 == 1 && baseLine[i].base != TokenBase.VAR_VARIABLE))
					errors.add("Invalid base declaration:" + baseLine[i].getErr());
				base += baseLine[i].text;
			}
		}
		
		// parse unit importations
		
		int importationCount = 1;
		while(importationCount < lines.length && lines[importationCount][0].base == TokenBase.DECL_IMPORT)
			importationCount++;
		importationCount--;
		
		importations = new String[importationCount];
		for(int i = 0; i < importationCount; i++) {
			Token[] line = lines[i+1];
			if(line.length < 2) {
				errors.add("Invalid importation declaration:" + line[0].getErr());
			} else if(line[line.length-1].base != TokenBase.VAR_UNIT) {
				errors.add("Invalid unit importation:" + line[line.length-1].getErr());
			} else {
				String importation = "";
				for(int j = 1; j < line.length-1; j++) {
					if((j%2 == 0 && line[j].base != TokenBase.TK_DOT) ||
						(j%2 == 1 && line[j].base != TokenBase.VAR_VARIABLE))
						errors.add("Invalid unit importation:" + line[j].getErr());
					importation += line[j].text;
				}
				importation += line[line.length-1].text;
				importations[i] = importation;
			}
		}
		
		// parse aliases
		
		int aliasCount = 2+importationCount;
		while(aliasCount < lines.length && lines[aliasCount][0].base == TokenBase.KW_ALIAS)
			aliasCount++;
		aliasCount -= 2+importationCount;
		
		int declarationEnd = 1+aliasCount+importationCount;
		
		// TODO check if the declaration end is right
		
		if(lines.length == declarationEnd) {
			errors.add("Incomplete source file:" + source.getErr(source.length()-1));
			errors.assertNoErrors();
		}
		
		Token[] unitLine = lines[1+importationCount];
		if(unitLine.length != 2 || unitLine[0].base != TokenBase.DECL_UNIT) {
			errors.add("Missing unit declaration!" + unitLine[0].getErr());
		} else if(unitLine[1].base != TokenBase.VAR_UNIT) {
			errors.add("Invalid unit declaration:" + unitLine[1].getErr());
		} else {
			name = unitLine[1].text;
		}
		errors.assertNoErrors();
		
		return new Unit(
				source,
				base,
				name,
				importations,
				aliasCount);
	}
	
	/** Assumes no line is empty */
	private static void parseSections(Unit unit, Token[][] lines, int headerEnd, ErrorWrapper errors) {
		List<FunctionSection> functions = new ArrayList<>();
		List<VariableDeclaration> variables = new ArrayList<>();
		List<StructSection> structures = new ArrayList<>();
		
		List<Modifier> modifiers = new ArrayList<>();
		
		for(int i = headerEnd; i < lines.length; i++) {
			Token[] line = lines[i];
			
			if(line[0].base == TokenBase.KW_FUNC) {
				DeclarationModifiers mods = new DeclarationModifiers(modifiers.toArray(Modifier[]::new));
				modifiers.clear();
				// parse function section
				if(!expectToken(line[line.length-1], TokenBase.TK_BRACE_OPEN, "Expected '{' to begin function", errors))
					continue;
				int functionEnd = getSectionEnd(lines, line[line.length-1].sectionPair, i);
				if(functionEnd == -1) {
					errors.add("Unfinished function:" + unit.source.getErr(lines[i]));
					continue;
				}
				FunctionSection func = FunctionDeclarationParser.parseFunctionSection(
						unit, lines, i, functionEnd, mods, errors);
				i = functionEnd;
				functions.add(func);
				
			} else if(line[0].base == TokenBase.VAR_MODIFIER) {
				// parse modifier
				modifiers.add(parseModifier(line, errors));
				
			} else if(line[0].base == TokenBase.KW_STRUCT) {
				if(!modifiers.isEmpty())
					errors.add("Struct sections do not take modifiers:" + unit.source.getErr(line));
				modifiers.clear();
				// parse struct
				if(line.length < 3) {
					errors.add("Invalid struct declaration:" + unit.source.getErr(lines[i]));
					continue;
				} else if(!expectToken(line[2], TokenBase.TK_BRACE_OPEN, "Expected '{' to begin structure", errors)) {
					continue;
				}
				int structEnd = getSectionEnd(lines, line[line.length-1].sectionPair, i);
				if(structEnd == -1) {
					errors.add("Unfinished struct:" + unit.source.getErr(lines[i]));
					continue;
				}
				ErrorWrapper subErrors = errors.subErrrors("Cannot parse a struct declaration");
				StructSection struct = StructSectionParser.parseStruct(unit, lines, i, structEnd, subErrors);
				i = structEnd;
				structures.add(struct);
				
			} else if(Tokens.isVarType(line[0].base)) {
				// parse variable declaration
				VariableDeclaration var = StatementParser.parseVariableDeclaration(unit, line, errors);
				var.modifiers = new DeclarationModifiers(modifiers.toArray(Modifier[]::new));
				variables.add(var);
				modifiers.clear();
				
			} else {
				errors.add("Unexpected line begin token:" + unit.source.getErr(line));
			}
		}
		
		unit.variables = variables.toArray(VariableDeclaration[]::new);
		unit.functions = functions.toArray(FunctionSection[]::new);
		unit.structures = structures.toArray(StructSection[]::new);
	}
	
	private static int getSectionEnd(Token[][] lines, Token sectionStop, int start) {
		for(int i = start+1; i < lines.length; i++) {
			if(lines[i].length == 1 && lines[i][0] == sectionStop)
				return i;
		}
		return -1;
	}
	
	/** Assumes that the first line token base is VAR_MODIFIER */
	private static Modifier parseModifier(Token[] line, ErrorWrapper errors) {
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
