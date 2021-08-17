package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public class UnitParser extends AbstractParser {
	
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
		
		// TODO0 check if the declaration end is right
		
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
	
	public static void parseUnit(Unit unit, Token[][] tokens, ErrorWrapper errors) throws WrappedException {
		int declarationEnd = unit.importations.length + unit.declaredAliasCount + 2;
		parseSections(unit, tokens, declarationEnd, errors.subErrrors("Unable to parse unit body"));
		errors.assertNoErrors();
	}
	
	/** Assumes no line is empty */
	private static void parseSections(Unit unit, Token[][] lines, int headerEnd, ErrorWrapper errors) {
		List<FunctionSection> functions = new ArrayList<>();
		List<VariableDeclaration> variables = new ArrayList<>();
		List<StructSection> structures = new ArrayList<>();
		
		ModifiersHolder modifiers = new ModifiersHolder();
		
		for(int i = headerEnd; i < lines.length; i++) {
			Token[] line = lines[i];
			
			if(line[0].base == TokenBase.KW_FUNC) {
				// parse function section
				DeclarationModifiers mods = modifiers.getModifiers();
				if(line[line.length-1].base != TokenBase.TK_BRACE_OPEN) {
					errors.add("Expected '{' to begin function:" + line[line.length-1].getErr());
					continue;
				}
				int functionEnd = getSectionEnd(lines, line[line.length-1].sectionPair, i);
				FunctionSection func = FunctionDeclarationParser.parseFunctionSection(
						unit, lines, i, functionEnd, mods, errors);
				i = functionEnd;
				functions.add(func);
				
			} else if(line[0].base == TokenBase.VAR_MODIFIER) {
				// parse modifier
				modifiers.add(parseModifier(line, errors));
				
			} else if(line[0].base == TokenBase.KW_STRUCT) {
				// parse struct
				DeclarationModifiers mods = modifiers.getModifiers();
				try {
					Pointer p = new Pointer(1);
					assertHasNext(line, p, "Invalid struct declaration", errors);
					assertToken(line, p, TokenBase.VAR_UNIT, "Expected structure name", errors);
					assertToken(line, p, TokenBase.TK_BRACE_OPEN, "Expected '{' to begin structure", errors);
					assertNoRemainingTokens(line, p, errors);
				} catch (ParsingException e) {
					continue;
				}
				int structEnd = getSectionEnd(lines, line[line.length-1].sectionPair, i);
				ErrorWrapper subErrors = errors.subErrrors("Cannot parse a struct declaration");
				StructSection struct = StructSectionParser.parseStruct(unit, lines, i, structEnd, mods, subErrors);
				i = structEnd;
				structures.add(struct);
				
			} else if(Tokens.isVarType(line[0].base)) {
				// parse variable declaration
				DeclarationModifiers mods = modifiers.getModifiers();
				VariableDeclaration var = StatementParser.parseVariableDeclaration(unit, line, mods, errors);
				variables.add(var);
				
			} else if(Tokens.isDeclarationVisibility(line[0].base)) {
				// parse visibility
				modifiers.setVisibility(line[0], errors);
				
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
	
}
