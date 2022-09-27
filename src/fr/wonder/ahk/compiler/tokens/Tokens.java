package fr.wonder.ahk.compiler.tokens;

import static fr.wonder.ahk.compiler.tokens.SectionToken.SEC_BRACES;
import static fr.wonder.ahk.compiler.tokens.SectionToken.SEC_BRACKETS;
import static fr.wonder.ahk.compiler.tokens.SectionToken.SEC_PARENTHESIS;
import static fr.wonder.ahk.compiler.tokens.TokenBase.*;

import java.util.Map;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.utils.NonNullableMap;
import fr.wonder.commons.utils.Assertions;

public class Tokens {
	
	public static final SectionToken[] SECTIONS = SectionToken.values();
	
	/** All of these must have different start and stop token bases */
	public static final SectionToken[] CODE_SECTIONS = {
			SEC_PARENTHESIS,
			SEC_BRACES,
			SEC_BRACKETS,
	};
	
	// the order matters for the tokenizer
	public static final TokenBase[] BASES = {
			LIT_INT, LIT_FLOAT, LIT_BOOL_TRUE,				// literals
			LIT_BOOL_FALSE, LIT_NULL,
			DECL_BASE, DECL_IMPORT, DECL_UNIT,				// declarations
			KW_VAR, KW_IF, KW_ELSE, KW_FOR, KW_FOREACH,		// keywords
			KW_WHILE, KW_FUNC, KW_STRUCT, KW_CONSTRUCTOR,
			KW_RETURN, KW_SIZEOF, KW_ALIAS, KW_GLOBAL,
			KW_LOCAL, KW_OPERATOR, KW_DOWHILE, KW_ENUM,
			TYPE_VOID, TYPE_INT, TYPE_FLOAT, TYPE_STR,		// types
			TYPE_BOOL,
			VAR_UNIT, VAR_VARIABLE, VAR_MODIFIER,			// variable elements (MUST be read last by the tokenizer)
	};
	
	public static final TokenBase[] SPLITS = {
			TK_LINE_BREAK,
			TK_BRACE_OPEN,
			TK_BRACE_CLOSE,
	};
	
	/** Keywords that can be used with/without a parenthesis header and have a body of a single line or multiple enclosed with braces */
	public static final TokenBase[] SINGLE_LINE_KEYWORDS = {
			KW_IF,
			KW_ELSE,
			KW_FOR,
			KW_FOREACH,
			KW_WHILE,
			KW_DOWHILE,
	};
	
	public static boolean isExternalDeclaration(TokenBase base) {
		return base == VAR_STRUCT ||
				base == VAR_ENUM;
	}

	public static boolean isLiteral(TokenBase base) {
		return base == LIT_BOOL_FALSE ||
				base == LIT_BOOL_TRUE ||
				base == LIT_INT ||
				base == LIT_FLOAT ||
				base == LIT_STR;
	}
	
	/* ----------------------------- Type Tokens ---------------------------- */

	public static boolean isVarType(TokenBase base) {
		return base == VAR_STRUCT || base == VAR_ENUM_NAME || typesMap.containsKey(base);
	}

	public static final Map<TokenBase, VarType> typesMap = NonNullableMap.of(
			TYPE_INT,	VarType.INT,
			TYPE_FLOAT,	VarType.FLOAT,
			TYPE_STR,	VarType.STR,
			TYPE_BOOL,	VarType.BOOL
	);
	
	/* ----------------------------- Type Tokens ---------------------------- */
	
	public static boolean isDeclarationVisibility(TokenBase base) {
		return visibilitiesMap.containsKey(base);
	}
	
	public static DeclarationVisibility getDeclarationVisibility(TokenBase base) {
		return visibilitiesMap.get(base);
	}
	
	private static final Map<TokenBase, DeclarationVisibility> visibilitiesMap = NonNullableMap.of(
			KW_LOCAL, DeclarationVisibility.LOCAL,
			KW_GLOBAL, DeclarationVisibility.GLOBAL
	);
	
	/* ------------------------- Affectation Tokens ------------------------- */

	public static boolean isAffectationOperator(TokenBase base) {
		return base == KW_EQUAL || equalsOperatorMap.containsKey(base);
	}
	
	/**
	 * Used to get the operator corresponding with an 'equal-operator' token (like +=...)
	 * @see #getOperator(TokenBase)
	 */
	public static Operator getAffectationOperator(TokenBase base) {
		return equalsOperatorMap.get(base);
	}

	private static final Map<TokenBase, Operator> equalsOperatorMap = NonNullableMap.of(
			KW_EQUAL_PLUS, 	Operator.ADD,
			KW_EQUAL_MINUS,	Operator.SUBSTRACT,
			KW_EQUAL_MUL, 	Operator.MULTIPLY,
			KW_EQUAL_DIV, 	Operator.DIVIDE,
			KW_EQUAL_MOD,	Operator.MOD
	);

	/* ---------------------- Direct Affectation Tokens --------------------- */

	public static boolean isDirectAffectation(TokenBase base) {
		return directOperatorsMap.containsKey(base);
	}

	public static Operator getDirectOperation(TokenBase base) {
		return directOperatorsMap.get(base);
	}

	private static final Map<TokenBase, Operator> directOperatorsMap = NonNullableMap.of(
			OP_DIRECT_PLUS,	Operator.ADD,
			OP_DIRECT_MINUS,Operator.SUBSTRACT
	);
	
	/* --------------------------- Operator Tokens -------------------------- */
	
	public static boolean isOperator(TokenBase base) {
		return operatorsMap.containsKey(base);
	}
	
	/**
	 * Used to get the operator corresponding with an operator token (like +, -, ==...)
	 */
	public static Operator getOperator(TokenBase base) {
		return operatorsMap.get(base);
	}

	private static final Map<TokenBase, Operator> operatorsMap = NonNullableMap.of(
			OP_PLUS,        Operator.ADD,
			OP_MINUS,       Operator.SUBSTRACT,
			OP_MUL,         Operator.MULTIPLY,
			OP_DIV,         Operator.DIVIDE,
			OP_EQUALS,      Operator.EQUALS,
			OP_SEQUALS,     Operator.STRICTEQUALS,
			OP_LEQUALS,     Operator.LEQUALS,
			OP_LOWER,       Operator.LOWER,
			OP_GREATER,     Operator.GREATER,
			OP_GEQUALS,     Operator.GEQUALS,
			OP_NEQUALS,     Operator.NEQUALS,
			OP_MOD,         Operator.MOD,
			OP_NOT,         Operator.NOT,
			OP_SHR,         Operator.SHR,
			OP_SHL,         Operator.SHL,
			OP_POWER,       Operator.POWER,
			OP_AND,         Operator.AND,
			OP_OR,          Operator.OR,
			OP_BITWISE_AND, Operator.BITWISE_AND,
			OP_BITWISE_OR,  Operator.BITWISE_OR
	);

	/* --------------------------- Utility Methods -------------------------- */
	
	public static final String extractEnumName(Token enumToken) {
		if(enumToken.base == TokenBase.VAR_ENUM)
			return enumToken.text.substring(0, enumToken.text.indexOf(':'));
		else if(enumToken.base == TokenBase.VAR_ENUM_NAME)
			return enumToken.text;
		throw new IllegalArgumentException("Not an enum token " + enumToken);
	}
	
	public static final String extractEnumValue(Token enumToken) {
		Assertions.assertTrue(enumToken.base == TokenBase.VAR_ENUM);
		return enumToken.text.substring(enumToken.text.lastIndexOf(':') + 1);
	}
}
