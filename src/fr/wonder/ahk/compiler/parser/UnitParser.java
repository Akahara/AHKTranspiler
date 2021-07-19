package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.Modifier;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public class UnitParser {
	
	public static Unit parseUnit(UnitSource source, ErrorWrapper errors) throws WrappedException {
		Token[] tokens = Tokenizer.tokenize(source, errors);
		errors.assertNoErrors();
		
		Token[][] unitTokens = TokensFactory.splitTokens(tokens);
		unitTokens = TokensFactory.finalizeTokens(unitTokens);
		
		UnitHeader header = parseHeader(source, unitTokens, errors.subErrrors("Unable to parse unit header"));
		errors.assertNoErrors();
		
		Unit unit = new Unit(source,
				header.base,
				header.name,
				header.unitDeclarationStart,
				header.unitDeclarationStop,
				header.importations);
		
		parseSections(unit, unitTokens, header.declarationEnd, errors.subErrrors("Unable to parse unit body"));
		errors.assertNoErrors();
		
		return unit;
	}
	
	private static class UnitHeader {
		
		String base;
		String name;
		String[] importations;
		
		int unitDeclarationStart, unitDeclarationStop;
		
		int declarationEnd;
		
	}
	
	private static UnitHeader parseHeader(UnitSource source, Token[][] lines, ErrorWrapper errors) {
		UnitHeader header = new UnitHeader();
		
		if(lines.length < 2) {
			errors.add("Incomplete ahk file:" + source.getErr(0));
			return null;
		}
		
		// parse unit base
		
		Token[] baseLine = lines[0];
		
		if(baseLine.length < 2 || baseLine[0].base != TokenBase.DECL_BASE) {
			errors.add("Missing header declaration!" + baseLine[0].getErr());
		} else {
			header.base = "";
			for(int i = 1; i < baseLine.length; i++) {
				if((i%2 == 0 && baseLine[i].base != TokenBase.TK_DOT) ||
					(i%2 == 1 && baseLine[i].base != TokenBase.VAR_VARIABLE))
					errors.add("Invalid base declaration:" + baseLine[i].getErr());
				header.base += baseLine[i].text;
			}
		}
		
		// parse unit importations
		
		int importationCount = 1;
		while(importationCount < lines.length && lines[importationCount][0].base == TokenBase.DECL_IMPORT)
			importationCount++;
		importationCount--;
		
		header.importations = new String[importationCount];
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
				header.importations[i] = importation;
			}
		}
		
		header.declarationEnd = 2+importationCount;
		
		// parse unit name
		
		if(lines.length == 1+importationCount) {
			errors.add("Incomplete source file:" + source.getErr(source.length()-1));
			return null;
		}
		
		Token[] unitLine = lines[1+importationCount];
		if(unitLine.length != 2 || unitLine[0].base != TokenBase.DECL_UNIT) {
			errors.add("Missing unit declaration!" + unitLine[0].getErr());
		} else if(unitLine[1].base != TokenBase.VAR_UNIT) {
			errors.add("Invalid unit declaration:" + unitLine[1].getErr());
		} else {
			header.name = unitLine[1].text;
			header.unitDeclarationStart = unitLine[0].sourceStart;
			header.unitDeclarationStop = unitLine[1].sourceStop;
		}
		
		return header;
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
				// parse function section
				if(line[line.length-1].base != TokenBase.TK_BRACE_OPEN) {
					errors.add("Expected '{' to begin function" + line[line.length-1].getErr());
				} else {
					int functionEnd = getSectionEnd(lines, line[line.length-1].sectionPair, i);
					if(functionEnd == -1) {
						errors.add("Unfinished function:" + unit.source.getErr(lines[i]));
					} else {
						DeclarationModifiers mods = new DeclarationModifiers(modifiers.toArray(Modifier[]::new));
						ErrorWrapper subErrors = errors.subErrrors("Invalid struct declaration");
						FunctionSection func = FunctionDeclarationParser.parseFunctionSection(
								unit, lines, i, functionEnd, mods, subErrors);
						i = functionEnd;
						functions.add(func);
						modifiers.clear();
					}
				}
				
			} else if(line[0].base == TokenBase.VAR_MODIFIER) {
				// parse modifier
				modifiers.add(parseModifier(line, errors));
				
			} else if(line[0].base == TokenBase.KW_STRUCT) {
				// parse struct
				if(line.length < 3) {
					errors.add("Invalid struct declaration:" + unit.source.getErr(lines[i]));
				} else if(line[2].base != TokenBase.TK_BRACE_OPEN) {
					errors.add("Expected '{' to begin struct:" + line[2].getErr());
				} else {
					int structEnd = getSectionEnd(lines, line[line.length-1].sectionPair, i);
					if(structEnd == -1) {
						errors.add("Unfinished struct:" + unit.source.getErr(lines[i]));
					} else {
						DeclarationModifiers mods = new DeclarationModifiers(modifiers.toArray(Modifier[]::new));
						StructSection struct = StructSectionParser.parseStruct(unit, lines, i, structEnd, mods, errors);
						i = structEnd;
						structures.add(struct);
						modifiers.clear();
					}
				}
				
			} else if(Tokens.isVarType(line[0].base)) {
				// parse variable declaration
				VariableDeclaration var = StatementParser.parseVariableDeclaration(unit, line, errors);
				if(var != null)
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