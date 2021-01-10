package fr.wonder.ahk.compiler.tokens;

import static fr.wonder.ahk.compiler.tokens.TokenBase.*;

public enum SectionToken {
	
	/* The order matter for the tokenizer !! */
	
	SEC_SPACE		(TK_SPACE, Mod.REPEATABLE),
	SEC_LINE_BREAK	(TK_LINE_BREAK, Mod.REPEATABLE),
	SEC_QUOTES1		(TK_DOUBLE_QUOTE, Mod.QUOTE),
	SEC_QUOTES2		(TK_APOSTROPHE, Mod.QUOTE),
	SEC_QUOTES3		(TK_BACK_APOSTROPHE, Mod.QUOTE),
	SEC_COMMENTS	(TK_COMMENT_OPEN, TK_COMMENT_CLOSE, Mod.QUOTE),
	
	SEC_DOUBLE_DOT	(TK_DOUBLE_DOT),
	SEC_DOT			(TK_DOT),
	SEC_COMA		(TK_COMA),
	SEC_COLUMN		(TK_COLUMN),
	
	SEC_BRACES		(TK_BRACE_OPEN, TK_BRACE_CLOSE),
	SEC_BRACKETS	(TK_BRACKET_OPEN, TK_BRACKET_CLOSE),
	SEC_PARENTHESIS	(TK_PARENTHESIS_OPEN, TK_PARENTHESIS_CLOSE),
	
	/*
	 * do not mistake a direct operator with its 'indirect' counterpart,
	 * direct operators are ++ -- and are used as shorthands for '+= 1'...
	 */
	SEC_OP_DIRECT_PLUS	(OP_DIRECT_PLUS),
	SEC_OP_DIRECT_MINUS	(OP_DIRECT_MINUS),
	/* 
	 * do not mistake an operator (OP_) for a keyword (KW), the latter looks like "+="
	 * and must be placed BEFORE both operators "+" and "="
	 */
	SEC_OP_SEQUALS		(OP_SEQUALS),
	SEC_OP_EQUALS		(OP_EQUALS),
	SEC_OP_GEQUALS		(OP_GEQUALS),
	SEC_OP_GREATER		(OP_GREATER),
	SEC_OP_LEQUALS		(OP_LEQUALS),
	SEC_OP_LOWER		(OP_LOWER),
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
		this.quote = mod == Mod.QUOTE;
		this.repeatable = mod == Mod.REPEATABLE;
	}
	
}
