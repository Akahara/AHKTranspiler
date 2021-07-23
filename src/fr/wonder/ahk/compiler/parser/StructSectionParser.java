package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.ConstructorDefaultValue;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.StructConstructor;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.parser.ExpressionParser.Section;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.exceptions.ErrorWrapper;

class StructSectionParser {

	public static StructSection parseStruct(Unit unit, Token[][] lines, int begin, int end,
			DeclarationModifiers modifiers, ErrorWrapper errors) {
		Token[] declaration = lines[begin];
		if(declaration.length != 3) {
			errors.add("Invalid declaration:" + unit.source.getErr(declaration));
			return Invalids.STRUCT;
		} else if(declaration[1].base != TokenBase.VAR_UNIT) {
			errors.add("Expected struct name:" + declaration[1].getErr());
			return Invalids.STRUCT;
		}
		
		List<VariableDeclaration> members = new ArrayList<>();
		List<StructConstructor> constructors = new ArrayList<>();
		ConstructorDefaultValue[] nullFields = null;
		
		for(int i = begin+1; i < end; i++) {
			Token[] line = lines[i];
			
			if(Tokens.isVarType(line[0].base)) {
				members.add(StatementParser.parseVariableDeclaration(unit, line, errors));
			} else if(line[0].base == TokenBase.KW_CONSTRUCTOR) {
				constructors.add(parseConstructor(unit, line, errors));
			} else if(line[0].base == TokenBase.LIT_NULL) {
				if(nullFields != null)
					errors.add("Null defined twice:" + unit.source.getErr(line));
				nullFields = parseNull(unit, line, errors);
			} else {
				errors.add("Unexpected line begin token in struct declaration:" + unit.source.getErr(line));
			}
		}
		
		if(constructors.isEmpty()) {
			constructors.add(new StructConstructor(
					unit.source,
					declaration[0].sourceStart,
					declaration[declaration.length-1].sourceStop,
					new FunctionArgument[0]));
		}
		
		if(nullFields == null)
			nullFields = new ConstructorDefaultValue[0];
		
		String structName = declaration[1].text;
		return new StructSection(
				unit.source,
				declaration[0].sourceStart,
				declaration[declaration.length-1].sourceStop,
				structName,
				modifiers,
				members.toArray(VariableDeclaration[]::new),
				constructors.toArray(StructConstructor[]::new),
				nullFields);
	}
	
	/** assumes that the line starts with the 'constructor' keyword */
	private static StructConstructor parseConstructor(Unit unit, Token[] line, ErrorWrapper errors) {
		if(line.length < 3 || line[line.length-1].base != TokenBase.TK_PARENTHESIS_CLOSE) {
			errors.add("Invalid constructor declaration:" + unit.source.getErr(line));
			return Invalids.CONSTRUCTOR;
		}
		
		var composite = FunctionDeclarationParser.readArguments(unit, line, 1, errors);
		if(composite.b != line.length) {
			errors.add("Unexpected tokens:" + unit.source.getErr(line, composite.b, line.length));
			return Invalids.CONSTRUCTOR;
		}
		
		FunctionArgument[] arguments = new FunctionArgument[composite.a.size()];
		for(int i = 0; i < arguments.length; i++)
			arguments[i] = composite.a.get(i).asFunctionArgument(unit.source);
		return new StructConstructor(
				unit.source,
				line[0].sourceStart,
				line[line.length-1].sourceStop,
				arguments);
	}
	
	private static ConstructorDefaultValue[] parseNull(Unit unit, Token[] line, ErrorWrapper errors) {
		if(line.length < 3) {
			errors.add("Invalid null declaration:" + unit.source.getErr(line));
			return null;
		} else if(line[1].base != TokenBase.TK_PARENTHESIS_OPEN) {
			errors.add("Expected '(':" + line[2].getErr());
			return null;
		} else if(line[line.length-1].base != TokenBase.TK_PARENTHESIS_CLOSE) {
			errors.add("Expected ')':" + line[line.length-1].getErr());
			return null;
		}
		Section section = ExpressionParser.getVisibleSection(line, 2, line.length-1);
		
		int prevComa = 2;
		List<ConstructorDefaultValue> defaultValues = new ArrayList<>();
		for(int i = 2; i < line.length-1; i = section.advancePointer(i)) {
			if(line[i].base == TokenBase.TK_COMMA) {
				ConstructorDefaultValue defaultValue = readDefaultValue(unit, line, prevComa, i, errors);
				if(defaultValue != null)
					defaultValues.add(defaultValue);
				prevComa = ++i;
			}
		}
		ConstructorDefaultValue defaultValue = readDefaultValue(unit, line, prevComa, line.length-1, errors);
		if(defaultValue != null)
			defaultValues.add(defaultValue);
		
		return defaultValues.toArray(ConstructorDefaultValue[]::new);
	}
	
	private static ConstructorDefaultValue readDefaultValue(Unit unit, Token[] line, int prevComa, int i, ErrorWrapper errors) {
		if(i - prevComa < 3) {
			errors.add("Invalid default value syntax:" + unit.source.getErr(line, prevComa, i));
		} else if(line[prevComa].base != TokenBase.VAR_VARIABLE) {
			errors.add("Expected default value name:" + line[prevComa].getErr());
		} else if(line[prevComa+1].base != TokenBase.KW_EQUAL) {
			errors.add("Expected '=':" + line[prevComa+1].getErr());
		} else {
			Expression value = ExpressionParser.parseExpression(unit, line, prevComa+2, i, errors);
			return new ConstructorDefaultValue(
					unit.source,
					line[prevComa].sourceStart,
					line[i-1].sourceStop,
					line[prevComa].text,
					value);
		}
		return null;
	}

}
