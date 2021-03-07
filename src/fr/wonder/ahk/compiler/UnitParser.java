package fr.wonder.ahk.compiler;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.LiteralExp;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.Statement;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.Modifier;
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
		
		UnitBody body = parseSections(source, unitTokens, header.declarationEnd, errors.subErrrors("Unable to parse unit body"));
		errors.assertNoErrors();
		
		Unit unit = new Unit(source, header.base, header.name, header.unitDeclarationStart, header.unitDeclarationStop,
				header.importations, body.variables, body.functions);
		
		return unit;
	}
	
	private static class UnitHeader {
		
		String base;
		String name;
		String[] importations;
		
		int unitDeclarationStart, unitDeclarationStop;
		
		int declarationEnd;
		
	}
	
	private static class UnitBody {
		
		VariableDeclaration[] variables;
		FunctionSection[] functions;
		
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
	private static UnitBody parseSections(UnitSource source, Token[][] lines, int headerEnd, ErrorWrapper errors) {
		List<FunctionSection> functions = new ArrayList<>();
		List<VariableDeclaration> variables = new ArrayList<>();
		
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
						errors.add("Unfinished function:" + source.getErr(lines[i]));
					} else {
						FunctionSection func = parseFunctionSection(source, lines, i, functionEnd, errors);
						i = functionEnd;
						if(func != null)
							func.modifiers = new DeclarationModifiers(modifiers.toArray(Modifier[]::new));
						functions.add(func);
					}
				}
				modifiers.clear();
				
			} else if(line[0].base == TokenBase.VAR_MODIFIER) {
				// parse modifier
				modifiers.add(parseModifier(line, errors));
				
			} else if(line[0].base == TokenBase.KW_STRUCT) {
				// parse struct
				errors.add("Structures are not yet supported!" + line[0].getErr());
				
			} else if(Tokens.isVarType(line[0].base) && line[0].base != TokenBase.TYPE_VOID) {
				// parse variable declaration
				VariableDeclaration var = StatementParser.parseVariableDeclaration(source, line, errors);
				if(var != null)
					var.modifiers = new DeclarationModifiers(modifiers.toArray(Modifier[]::new));
				variables.add(var);
				modifiers.clear();
				
			} else {
				errors.add("Unexpected line start token:" + line[0].getErr());
			}
		}
		
		UnitBody body = new UnitBody();
		body.variables = variables.toArray(VariableDeclaration[]::new);
		body.functions = functions.toArray(FunctionSection[]::new);
		return body;
	}
	
	private static int getSectionEnd(Token[][] lines, Token sectionStop, int start) {
		for(int i = start+1; i < lines.length; i++) {
			if(lines[i].length == 1 && lines[i][0] == sectionStop)
				return i;
		}
		return -1;
	}
	
	/** Assumes that the first line token is KW_FUNC and the last TK_BRACE_OPEN */
	private static FunctionSection parseFunctionSection(UnitSource source, Token[][] tokens,
			int start, int stop, ErrorWrapper errors) {
		Token[] declaration = tokens[start];
		FunctionSection function = new FunctionSection(
				source,
				declaration[0].sourceStart, // source start
				tokens[stop-1][tokens[stop-1].length-1].sourceStop, // source stop
				declaration[declaration.length-1].sourceStop); // declaration stop
		if(declaration.length < 6) {
			errors.add("Incomplete function declaration:" + function.getErr());
			return function;
		}
		ErrorWrapper declErrors = errors.subErrrors("Invalid function declaration");
		if(declaration[2].base != TokenBase.VAR_VARIABLE)
			declErrors.add("Expected function name:" + declaration[2].getErr());
		if(declaration[3].base != TokenBase.TK_PARENTHESIS_OPEN)
			declErrors.add("Expected '('" + declaration[3].getErr());
		if(declaration[declaration.length-2].base != TokenBase.TK_PARENTHESIS_CLOSE)
			declErrors.add("Expected ')'" + declaration[declaration.length-1].getErr());
		if(!declErrors.noErrors())
			return function;
		function.returnType = Tokens.getType(declaration[1]);
		function.name = declaration[2].text;
		function.arguments = new FunctionArgument[(declaration.length-4)/3];
		function.argumentTypes = new VarType[function.arguments.length];
		if(function.returnType == null)
			errors.add("Unknown return value type:" + declaration[1].getErr());
		for(int i = 4; i < declaration.length-2; i++) {
			if((i-4)%3 == 0) {
				if(!Tokens.isVarType(declaration[i].base)) {
					declErrors.add("Expected argument type:" + declaration[i].getErr());
				} else if(declaration[i+1].base != TokenBase.VAR_VARIABLE) {
					declErrors.add("Expected argument name:" + declaration[i+1].getErr());
				} else {
					VarType argType = Tokens.getType(declaration[i]);
					int argIdx = (i-4)/3;
					function.arguments[argIdx] = new FunctionArgument(declaration[i].sourceStart,
							declaration[i+1].sourceStop, declaration[i+1].text, argType);
					function.argumentTypes[argIdx] = argType;
					
					if(argType == null)
						errors.add("Unknown argument type:" + declaration[i].getErr());
				}
			} else if((i-4)%3 == 2) {
				if(declaration[i].base != TokenBase.TK_COMA)
					declErrors.add("Expected ','" + declaration[i].getErr());
			}
		}
		if(function.arguments.length != 0 && declaration.length%3 != 2)
			declErrors.add("Unexpected token" + declaration[declaration.length-3].getErr());
		
		function.body = new Statement[stop-start-1];
		ErrorWrapper functionErrors = errors.subErrrors("Unable to parse function");
		for(int i = start+1; i < stop; i++)
			function.body[i-start-1] = StatementParser.parseStatement(source, tokens[i], functionErrors);
		if(functionErrors.noErrors())
			StatementParser.finalizeStatements(source, function);
//		Utils.dump(Arrays.copyOfRange(tokens, start, stop));
		return function;
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
		} else {
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
					if(line[i].base != TokenBase.TK_COMA)
						errors.add("Invalid modifier syntax:" + line[i].getErr());
				}
			}
			return new Modifier(line[0].text.substring(1), arguments);
		}
		return null;
	}
	
}
