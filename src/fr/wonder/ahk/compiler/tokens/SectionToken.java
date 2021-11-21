package fr.wonder.ahk.compiler.tokens;

import static fr.wonder.ahk.compiler.tokens.TokenBase.*;

public enum SectionToken {
	
	/* The order matter for the tokenizer !! */
	
	SEC_SPACE		(TK_SPACE, Mod.REPEATABLE),
	SEC_NL			(TK_NL, Mod.REPEATABLE),
	SEC_LINE_BREAK	(TK_LINE_BREAK, Mod.REPEATABLE),
	SEC_QUOTES1		(TK_DOUBLE_QUOTE, TK_DOUBLE_QUOTE, Mod.QUOTE),
	SEC_QUOTES2		(TK_APOSTROPHE, TK_APOSTROPHE, Mod.QUOTE),
	SEC_QUOTES3		(TK_BACK_APOSTROPHE, TK_BACK_APOSTROPHE, Mod.QUOTE),
	SEC_COMMENTS	(TK_COMMENT_OPEN, TK_COMMENT_CLOSE, Mod.QUOTE),
	SEC_LINE_COMMENT(TK_LINE_COMMENT, TK_NL, Mod.QUOTE),
	
	SEC_DOUBLE_DOT	(TK_DOUBLE_DOT),
	SEC_DOT			(TK_DOT),
	SEC_COMA		(TK_COMMA),
	SEC_COLUMN		(TK_COLUMN),
	
	SEC_BRACES		(TK_BRACE_OPEN, TK_BRACE_CLOSE),
	SEC_BRACKETS	(TK_BRACKET_OPEN, TK_BRACKET_CLOSE),
	SEC_PARENTHESIS	(TK_PARENTHESIS_OPEN, TK_PARENTHESIS_CLOSE),
	
	SEC_GENERIC_BINDING (TK_GENERIC_BINDING_BEGIN, TK_GENERIC_BINDING_END),
	SEC_LAMBDA_ACTION	(TK_LAMBDA_ACTION),
	
	/*
	 * do not mistake a direct operator with its 'indirect' counterpart,
	 * direct operators are ++ -- and are used as shorthands for '+= 1'...
	 */
	SEC_OP_DIRECT_PLUS	(OP_DIRECT_PLUS),
	SEC_OP_DIRECT_MINUS	(OP_DIRECT_MINUS),
	/* 
	 * do not mistake an operator (OP_) for a keyword (KW_), the latter looks like "+="
	 * and must be placed BEFORE both operators "+" and "="
	 */
	SEC_OP_SHR			(OP_SHR),
	SEC_OP_SHL			(OP_SHL),
	SEC_OP_SEQUALS		(OP_SEQUALS),
	SEC_OP_EQUALS		(OP_EQUALS),
	SEC_OP_GEQUALS		(OP_GEQUALS),
	SEC_OP_GREATER		(OP_GREATER),
	SEC_OP_LEQUALS		(OP_LEQUALS),
	SEC_OP_LOWER		(OP_LOWER),
	SEC_OP_NEQUALS		(OP_NEQUALS),
	SEC_OP_NOT			(OP_NOT),
	SEC_KW_EQUAL_PLUS	(KW_EQUAL_PLUS),
	SEC_OP_PLUS			(OP_PLUS),
	SEC_KW_EQUAL_MINUS	(KW_EQUAL_MINUS),
	SEC_OP_MINUS		(OP_MINUS),
	SEC_KW_EQUAL_MUL	(KW_EQUAL_MUL),
	SEC_OP_MUL			(OP_MUL),
	SEC_KW_EQUAL_DIV	(KW_EQUAL_DIV),
	SEC_OP_DIV			(OP_DIV),
	SEC_KW_EQUAL_MOD	(KW_EQUAL_MOD),
	SEC_OP_MOD			(OP_MOD),
	SEC_KW_EQUAL		(KW_EQUAL),
	SEC_OP_POWER		(OP_POWER),
	SEC_OP_OR			(OP_OR),
	SEC_OP_AND			(OP_AND),
	SEC_OP_BITWISE_OR	(OP_BITWISE_OR),
	SEC_OP_BITWISE_AND	(OP_BITWISE_AND),

	;
	
	public final TokenBase start, stop;
	public final boolean repeatable, quote;
	
	private static class Mod {
		private final static int REPEATABLE = 1;
		private final static int QUOTE = 2;
	}
	
	private SectionToken(TokenBase base) {
		this(base, 0);
	}
	
	private SectionToken(TokenBase base, int mod) {
		this(base, null, mod);
	}
	
	private SectionToken(TokenBase start, TokenBase stop) {
		this(start, stop, 0);
	}
	
	private SectionToken(TokenBase start, TokenBase stop, int mod) {
		this.start = start;
		this.stop = stop;
		this.quote = (mod & Mod.QUOTE) != 0;
		this.repeatable = (mod &= Mod.REPEATABLE) != 0;
		if((mod & Mod.QUOTE) != 0 && stop == null)
			throw new IllegalArgumentException("Quote tokens must have an end base");
	}
	
}
