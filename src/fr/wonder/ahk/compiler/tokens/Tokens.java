package fr.wonder.ahk.compiler.tokens;

import static fr.wonder.ahk.compiler.tokens.SectionToken.SEC_BRACES;
import static fr.wonder.ahk.compiler.tokens.SectionToken.SEC_BRACKETS;
import static fr.wonder.ahk.compiler.tokens.SectionToken.SEC_PARENTHESIS;
import static fr.wonder.ahk.compiler.tokens.TokenBase.DECL_BASE;
import static fr.wonder.ahk.compiler.tokens.TokenBase.DECL_IMPORT;
import static fr.wonder.ahk.compiler.tokens.TokenBase.DECL_UNIT;
import static fr.wonder.ahk.compiler.tokens.TokenBase.KW_ELSE;
import static fr.wonder.ahk.compiler.tokens.TokenBase.KW_EQUAL;
import static fr.wonder.ahk.compiler.tokens.TokenBase.KW_EQUAL_DIV;
import static fr.wonder.ahk.compiler.tokens.TokenBase.KW_EQUAL_MINUS;
import static fr.wonder.ahk.compiler.tokens.TokenBase.KW_EQUAL_MOD;
import static fr.wonder.ahk.compiler.tokens.TokenBase.KW_EQUAL_MUL;
import static fr.wonder.ahk.compiler.tokens.TokenBase.KW_EQUAL_PLUS;
import static fr.wonder.ahk.compiler.tokens.TokenBase.KW_FOR;
import static fr.wonder.ahk.compiler.tokens.TokenBase.KW_FOREACH;
import static fr.wonder.ahk.compiler.tokens.TokenBase.KW_FUNC;
import static fr.wonder.ahk.compiler.tokens.TokenBase.KW_IF;
import static fr.wonder.ahk.compiler.tokens.TokenBase.KW_RETURN;
import static fr.wonder.ahk.compiler.tokens.TokenBase.KW_SIZEOF;
import static fr.wonder.ahk.compiler.tokens.TokenBase.KW_STRUCT;
import static fr.wonder.ahk.compiler.tokens.TokenBase.KW_VAR;
import static fr.wonder.ahk.compiler.tokens.TokenBase.KW_WHILE;
import static fr.wonder.ahk.compiler.tokens.TokenBase.LIT_BOOL_FALSE;
import static fr.wonder.ahk.compiler.tokens.TokenBase.LIT_BOOL_TRUE;
import static fr.wonder.ahk.compiler.tokens.TokenBase.LIT_FLOAT;
import static fr.wonder.ahk.compiler.tokens.TokenBase.LIT_INT;
import static fr.wonder.ahk.compiler.tokens.TokenBase.LIT_NULL;
import static fr.wonder.ahk.compiler.tokens.TokenBase.LIT_STR;
import static fr.wonder.ahk.compiler.tokens.TokenBase.OP_DIRECT_MINUS;
import static fr.wonder.ahk.compiler.tokens.TokenBase.OP_DIRECT_PLUS;
import static fr.wonder.ahk.compiler.tokens.TokenBase.OP_DIV;
import static fr.wonder.ahk.compiler.tokens.TokenBase.OP_EQUALS;
import static fr.wonder.ahk.compiler.tokens.TokenBase.OP_GEQUALS;
import static fr.wonder.ahk.compiler.tokens.TokenBase.OP_GREATER;
import static fr.wonder.ahk.compiler.tokens.TokenBase.OP_LEQUALS;
import static fr.wonder.ahk.compiler.tokens.TokenBase.OP_LOWER;
import static fr.wonder.ahk.compiler.tokens.TokenBase.OP_MINUS;
import static fr.wonder.ahk.compiler.tokens.TokenBase.OP_MOD;
import static fr.wonder.ahk.compiler.tokens.TokenBase.OP_MUL;
import static fr.wonder.ahk.compiler.tokens.TokenBase.OP_NEQUALS;
import static fr.wonder.ahk.compiler.tokens.TokenBase.OP_NOT;
import static fr.wonder.ahk.compiler.tokens.TokenBase.OP_PLUS;
import static fr.wonder.ahk.compiler.tokens.TokenBase.OP_SEQUALS;
import static fr.wonder.ahk.compiler.tokens.TokenBase.OP_SHL;
import static fr.wonder.ahk.compiler.tokens.TokenBase.OP_SHR;
import static fr.wonder.ahk.compiler.tokens.TokenBase.TK_BRACE_CLOSE;
import static fr.wonder.ahk.compiler.tokens.TokenBase.TK_BRACE_OPEN;
import static fr.wonder.ahk.compiler.tokens.TokenBase.TK_LINE_BREAK;
import static fr.wonder.ahk.compiler.tokens.TokenBase.TK_SPACE;
import static fr.wonder.ahk.compiler.tokens.TokenBase.TYPE_BOOL;
import static fr.wonder.ahk.compiler.tokens.TokenBase.TYPE_FLOAT;
import static fr.wonder.ahk.compiler.tokens.TokenBase.TYPE_INT;
import static fr.wonder.ahk.compiler.tokens.TokenBase.TYPE_STR;
import static fr.wonder.ahk.compiler.tokens.TokenBase.TYPE_VOID;
import static fr.wonder.ahk.compiler.tokens.TokenBase.VAR_MODIFIER;
import static fr.wonder.ahk.compiler.tokens.TokenBase.VAR_UNIT;
import static fr.wonder.ahk.compiler.tokens.TokenBase.VAR_VARIABLE;

import java.util.HashMap;
import java.util.Map;

import fr.wonder.ahk.compiled.expressions.Operator;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiler.Invalids;

public class Tokens {
	
	public static final SectionToken[] SECTIONS = SectionToken.values();
	
	/** All of these must have different start and stop token bases */
	public static final SectionToken[] CODE_SECTIONS = {
			SEC_PARENTHESIS,
			SEC_BRACES,
			SEC_BRACKETS
	};
	
	// the order matters for the tokenizer
	public static final TokenBase[] BASES = {
			LIT_INT, LIT_FLOAT, LIT_BOOL_TRUE,					// literals
			LIT_BOOL_FALSE, LIT_NULL,
			DECL_BASE, DECL_IMPORT, DECL_UNIT,					// declarations
			KW_VAR, KW_IF, KW_ELSE, KW_FOR, KW_FOREACH,			// keywords
			KW_WHILE, KW_FUNC, KW_STRUCT, KW_RETURN,
			KW_SIZEOF,
			TYPE_VOID, TYPE_INT, TYPE_FLOAT, TYPE_STR,			// types
			TYPE_BOOL,
			VAR_UNIT, VAR_VARIABLE, VAR_MODIFIER,				// variable elements (MUST be read last by the tokenizer)
	};
	
	public static final TokenBase[] SPLITS = {
			TK_LINE_BREAK,
			TK_BRACE_OPEN,
			TK_BRACE_CLOSE
	};
	
	public static final TokenBase[] SPLIT_LOSSES = {
			TK_SPACE
	};
	
	/** Keywords that can be used with/without a parenthesis header and have a body of a single line or multiple enclosed with braces */
	public static final TokenBase[] SINGLE_LINE_KEYWORDS = {
			KW_IF,
			KW_ELSE,
			KW_FOR,
			KW_FOREACH,
			KW_WHILE
	};

	public static boolean isLiteral(TokenBase base) {
		return base == LIT_BOOL_FALSE ||
				base == LIT_BOOL_TRUE ||
				base == LIT_INT ||
				base == LIT_FLOAT ||
				base == LIT_STR;
	}
	
	/* ----------------------------- Type Tokens ---------------------------- */

	public static boolean isVarType(TokenBase base) {
		return base == VAR_UNIT || typesMap.containsKey(base);
	}

	public static VarType getType(Unit unit, Token token) {
		if(token.base == VAR_UNIT)
			return unit.getStructType(token);
		return typesMap.getOrDefault(token.base, Invalids.TYPE);
	}
	
	private static final Map<TokenBase, VarType> typesMap = Map.of(
			TYPE_INT,	VarType.INT,
			TYPE_FLOAT,	VarType.FLOAT,
			TYPE_STR,	VarType.STR,
			TYPE_BOOL,	VarType.BOOL
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

	private static final Map<TokenBase, Operator> equalsOperatorMap = Map.of(
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

	private static final Map<TokenBase, Operator> directOperatorsMap = Map.of(
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

	private static final Map<TokenBase, Operator> operatorsMap = new HashMap<>();
	static {
			operatorsMap.put(OP_PLUS,	Operator.ADD);
			operatorsMap.put(OP_MINUS,	Operator.SUBSTRACT);
			operatorsMap.put(OP_MUL,	Operator.MULTIPLY);
			operatorsMap.put(OP_DIV,	Operator.DIVIDE);
			operatorsMap.put(OP_EQUALS,	Operator.EQUALS);
			operatorsMap.put(OP_SEQUALS,Operator.SEQUALS);
			operatorsMap.put(OP_LEQUALS,Operator.LEQUALS);
			operatorsMap.put(OP_LOWER,	Operator.LOWER);
			operatorsMap.put(OP_GREATER,Operator.GREATER);
			operatorsMap.put(OP_GEQUALS,Operator.GEQUALS);
			operatorsMap.put(OP_NEQUALS,Operator.NEQUALS);
			operatorsMap.put(OP_MOD,	Operator.MOD);
			operatorsMap.put(OP_NOT,	Operator.NOT);
			operatorsMap.put(OP_SHR, 	Operator.SHR);
			operatorsMap.put(OP_SHL, 	Operator.SHL);
	}
}
