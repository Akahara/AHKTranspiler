package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.BlueprintRef;
import fr.wonder.ahk.compiled.units.sections.ConstructorDefaultValue;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.ahk.compiled.units.sections.GenericContext;
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

	/**
	 * Parses a structure declaration and body.
	 * 
	 * <p>
	 * Assumes that {@code lines[begin][0].base == KW_STRUCT}
	 */
	public static StructSection parseStruct(Unit unit, Token[][] lines, int begin, int end,
			DeclarationModifiers structModifiers, ErrorWrapper errors) {
		
		Token[] declaration = lines[begin];
		StructSection structure;
		try {
			Pointer p = new Pointer(1);

			String structName = assertToken(declaration, p, TokenBase.VAR_STRUCT, "Expected structure name", errors).text;
			GenericContext genc = readGenericArray(unit, declaration, null, p, errors);
			BlueprintRef[] implementedBlueprints;
			
			if(declaration[p.position].base == TokenBase.TK_COLUMN) {
				p.position++;
				implementedBlueprints = readGenericRestriction(unit, declaration, p, errors);
			} else {
				implementedBlueprints = new BlueprintRef[0];
			}
			
			if(p.position != declaration.length - 1) {
				errors.add("Unexpected tokens:" + SourceReference.fromLine(declaration, p.position, declaration.length-2).getErr());
			}
			
			structure = new StructSection(
					unit,
					SourceReference.fromLine(declaration),
					structName,
					genc,
					implementedBlueprints,
					structModifiers);
		} catch (ParsingException e) {
			return Invalids.STRUCTURE;
		}
		
		List<VariableDeclaration> members = new ArrayList<>();
		List<StructConstructor> constructors = new ArrayList<>();
		List<OverloadedOperator> operators = new ArrayList<>();
		ConstructorDefaultValue[] nullFields = null;
		
		ModifiersHolder modifiers = new ModifiersHolder();
		
		for(int i = begin+1; i < end; i++) {
			Token[] line = lines[i];
			
			if(Tokens.isVarType(line[0].base)) {
				members.add(StatementParser.parseVariableDeclaration(unit, line,
						structure.genericContext, modifiers.getModifiers(), errors));
				
			} else if(line[0].base == TokenBase.KW_CONSTRUCTOR) {
				constructors.add(parseConstructor(structure, line, modifiers.getModifiers(), errors));
				
			} else if(line[0].base == TokenBase.LIT_NULL) {
				if(nullFields != null)
					errors.add("Null defined twice:" + unit.source.getErr(line));
				nullFields = parseNull(unit, structure, line, errors);
				
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
					SourceReference.fromLine(declaration),
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
			ArgumentList args = readArguments(structure.unit, line,
					true, structure.genericContext, pointer, ALLOW_NONE, errors);
			FunctionArgument[] arguments = args.toArray(FunctionArgument[]::new);
			
			assertNoRemainingTokens(line, pointer, errors);
			
			return new StructConstructor(
					structure,
					SourceReference.fromLine(line),
					modifiers,
					arguments);
			
		} catch (ParsingException e) {
			return Invalids.CONSTRUCTOR;
		}
	}
	
	private static ConstructorDefaultValue[] parseNull(Unit unit, StructSection structure, Token[] line, ErrorWrapper errors) {
		if(line.length < 3) {
			errors.add("Invalid null declaration:" + unit.source.getErr(line));
			return null;
		}
		try { assertParentheses(line, 1, line.length-1, errors); }
		catch (ParsingException e) { return null; }
		Section section = ExpressionParser.getVisibleSection(line, 2, line.length-1);
		
		int prevComa = 2;
		List<ConstructorDefaultValue> defaultValues = new ArrayList<>();
		for(Pointer p = new Pointer(2); p.position < line.length-1; section.advancePointer(p)) {
			if(line[p.position].base == TokenBase.TK_COMMA) {
				ConstructorDefaultValue defaultValue = readDefaultValue(unit, structure, line, prevComa, p.position, errors);
				if(defaultValue != null)
					defaultValues.add(defaultValue);
				prevComa = ++p.position;
			}
		}
		ConstructorDefaultValue defaultValue = readDefaultValue(unit, structure, line, prevComa, line.length-1, errors);
		if(defaultValue != null)
			defaultValues.add(defaultValue);
		
		return defaultValues.toArray(ConstructorDefaultValue[]::new);
	}
	
	private static ConstructorDefaultValue readDefaultValue(Unit unit, StructSection struct,
			Token[] line, int prevComa, int i, ErrorWrapper errors) {
		
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
		Expression value = ExpressionParser.parseExpression(struct.unit, line, struct.genericContext, prevComa+2, i, errors);
		return new ConstructorDefaultValue(
				SourceReference.fromLine(line, prevComa, i-1),
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
				leftOperand = parseType(unit, line, structure.genericContext, p, ALLOW_NONE, errors);
			assertHasNext(line, p, "Expected overloaded operator", errors);
			Operator op = Tokens.getOperator(line[p.position].base);
			if(op == null)
				errors.add("Expected operator:" + line[p.position].getErr());
			else if(op == Operator.STRICTEQUALS)
				errors.add("The strict equality operator cannot be overridden:" + line[p.position].getErr());
			p.position++;
			assertHasNext(line, p, "Incomplete operator declaration", errors);
			VarType rightOperand = parseType(unit, line, structure.genericContext, p, ALLOW_NONE, errors);
			assertToken(line, p, TokenBase.KW_EQUAL, "Expected '='", errors);
			assertHasNext(line, p, "Expected operator result type", errors);
			VarType resultType = parseType(unit, line, structure.genericContext, p, ALLOW_NONE, errors);
			assertNoRemainingTokens(line, p, errors);
			
			return new OverloadedOperator(structure, SourceReference.fromLine(line),
					op, resultType, leftOperand, rightOperand, funcName);
		} catch (ParsingException e) {
			return Invalids.OVERLOADED_OPERATOR;
		}
	}

}
