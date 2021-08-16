package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.ConstructorDefaultValue;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.OverloadedOperator;
import fr.wonder.ahk.compiled.units.sections.StructConstructor;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.parser.ExpressionParser.Section;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.exceptions.ErrorWrapper;

class StructSectionParser extends AbstractParser {

	/* assumes that the lines[begin] is a valid struct declaration (kw_struct var_unit tk_{)*/
	public static StructSection parseStruct(Unit unit, Token[][] lines, int begin, int end,
			DeclarationModifiers structModifiers, ErrorWrapper errors) {
		
		Token[] declaration = lines[begin];
		String structName = declaration[1].text;
		
		StructSection structure = new StructSection(
				unit,
				declaration[0].sourceStart,
				declaration[declaration.length-1].sourceStop,
				structName,
				structModifiers);
		
		List<VariableDeclaration> members = new ArrayList<>();
		List<StructConstructor> constructors = new ArrayList<>();
		List<OverloadedOperator> operators = new ArrayList<>();
		ConstructorDefaultValue[] nullFields = null;
		
		ModifiersHolder modifiers = new ModifiersHolder();
		
		for(int i = begin+1; i < end; i++) {
			Token[] line = lines[i];
			
			if(Tokens.isVarType(line[0].base)) {
				members.add(StatementParser.parseVariableDeclaration(unit, line, modifiers.getModifiers(), errors));
				
			} else if(line[0].base == TokenBase.KW_CONSTRUCTOR) {
				constructors.add(parseConstructor(structure, line, modifiers.getModifiers(), errors));
				
			} else if(line[0].base == TokenBase.LIT_NULL) {
				if(nullFields != null)
					errors.add("Null defined twice:" + unit.source.getErr(line));
				nullFields = parseNull(unit, line, errors);
				
			} else if(line[0].base == TokenBase.VAR_MODIFIER) {
				modifiers.add(parseModifier(line, errors));
				
			} else if(Tokens.isDeclarationVisibility(line[0].base)) {
				modifiers.setVisibility(line[0], errors);
				
			} else if(line[0].base == TokenBase.KW_OPERATOR) {
				operators.add(parseOverloadedOperator(unit, structure, line, errors));
				
			} else {
				errors.add("Unexpected line begin token in struct declaration:" + unit.source.getErr(line));
			}
		}
		
		if(constructors.isEmpty()) {
			constructors.add(new StructConstructor(
					structure,
					declaration[0].sourceStart,
					declaration[declaration.length-1].sourceStop,
					DeclarationModifiers.NONE,
					new FunctionArgument[0]));
		}
		
		if(nullFields == null)
			nullFields = new ConstructorDefaultValue[0];

		structure.members = members.toArray(VariableDeclaration[]::new);
		structure.constructors = constructors.toArray(StructConstructor[]::new);
		structure.operators = operators.toArray(OverloadedOperator[]::new);
		structure.nullFields = nullFields;
		
		return structure;
	}

	/** assumes that the line starts with the 'constructor' keyword */
	private static StructConstructor parseConstructor(StructSection structure,
			Token[] line, DeclarationModifiers modifiers, ErrorWrapper errors) {
		
		if(line.length < 3 || line[line.length-1].base != TokenBase.TK_PARENTHESIS_CLOSE) {
			errors.add("Invalid constructor declaration:" + structure.unit.source.getErr(line));
			return Invalids.CONSTRUCTOR;
		}
		
		Pointer pointer = new Pointer(1);
		
		try {
			ArgumentList args = readArguments(structure.unit, line, pointer, true, errors);
			FunctionArgument[] arguments = args.toArray(FunctionArgument[]::new);
			
			assertNoRemainingTokens(line, pointer, errors);
			
			return new StructConstructor(
					structure,
					line[0].sourceStart,
					line[line.length-1].sourceStop,
					modifiers,
					arguments);
			
		} catch (ParsingException e) {
			return Invalids.CONSTRUCTOR;
		}
	}
	
	private static ConstructorDefaultValue[] parseNull(Unit unit, Token[] line, ErrorWrapper errors) {
		if(line.length < 3) {
			errors.add("Invalid null declaration:" + unit.source.getErr(line));
			return null;
		}
		try { assertParentheses(line, 1, line.length-1, errors); }
		catch (ParsingException e) { return null; }
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
			return null;
		}
		try {
			Pointer p = new Pointer(prevComa);
			assertToken(line, p, TokenBase.VAR_VARIABLE, "Expected default value name", errors);
			assertToken(line, p, TokenBase.KW_EQUAL, "Expected '='", errors);
		} catch (ParsingException e) {
			return null;
		}
		Expression value = ExpressionParser.parseExpression(unit, line, prevComa+2, i, errors);
		return new ConstructorDefaultValue(
				unit.source,
				line[prevComa].sourceStart,
				line[i-1].sourceStop,
				line[prevComa].text,
				value);
	}
	
	/** assumes that line[0].base == KW_OPERATOR */
	private static OverloadedOperator parseOverloadedOperator(Unit unit, StructSection structure, Token[] line, ErrorWrapper errors) {
		try {
			Pointer p = new Pointer(1);
			assertHasNext(line, p, "Incomplete operator declaration", errors, 7);
			String funcName = assertToken(line, p, TokenBase.VAR_VARIABLE, "Expected operator implementation function name", errors).text;
			assertToken(line, p, TokenBase.TK_COLUMN, "Expected ':'", errors);
			VarType leftOperand = null;
			if(!Tokens.isOperator(line[p.position].base))
				leftOperand = parseType(unit, line, p, errors);
			assertHasNext(line, p, "Expected overloaded operator", errors);
			Operator op = Tokens.getOperator(line[p.position].base);
			if(op == null)
				errors.add("Expected operator:" + line[p.position].getErr());
			p.position++;
			assertHasNext(line, p, "Incomplete operator declaration", errors);
			VarType rightOperand = parseType(unit, line, p, errors);
			assertToken(line, p, TokenBase.KW_EQUAL, "Expected '='", errors);
			assertHasNext(line, p, "Expected operator result type", errors);
			VarType resultType = parseType(unit, line, p, errors);
			assertNoRemainingTokens(line, p, errors);
			
			return new OverloadedOperator(structure, op,
					resultType, leftOperand, rightOperand, funcName,
					line[0].sourceStart, line[line.length-1].sourceStop);
		} catch (ParsingException e) {
			return Invalids.OVERLOADED_OPERATOR;
		}
	}

}
