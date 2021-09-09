package fr.wonder.ahk.compiler.tokens;


public enum TokenBase {
	
	/* Used as-is */
	
	VAR_UNIT("[A-Z]\\w*"),
	VAR_VARIABLE("[a-z]\\w*"),
	VAR_GENERIC("[A-Z]"),
	VAR_MODIFIER("@[a-z]\\w*"),
	
	LIT_INT("\\d+"),
	LIT_FLOAT("\\d+\\.\\d*"),
	LIT_STR(null),
	LIT_BOOL_TRUE("true"),
	LIT_BOOL_FALSE("false"),
	LIT_NULL("null"),
	
	DECL_BASE("base"),
	DECL_IMPORT("import"),
	DECL_UNIT("unit"),
	
	KW_VAR("var"),
	KW_IF("if"),
	KW_ELSE("else"),
	KW_FOR("for"),
	KW_FOREACH("foreach"),
	KW_WHILE("while"),
	KW_FUNC("func"),
	KW_STRUCT("struct"),
	KW_CONSTRUCTOR("constructor"),
	KW_RETURN("return"),
	KW_SIZEOF("sizeof"),
	KW_ALIAS("alias"),
	KW_GLOBAL("global"),
	KW_LOCAL("local"),
	KW_OPERATOR("operator"),
	KW_BLUEPRINT("blueprint"),
	
	TYPE_VOID("void"),
	TYPE_INT("int"),
	TYPE_FLOAT("float"),
	TYPE_STR("str"),
	TYPE_BOOL("bool"),
	
	/* Used as section tokens */
	
	/** not to mistake with {@link #OP_EQUALS}, this one is <code>=</code> */
	KW_EQUAL("="),
	KW_EQUAL_PLUS("+="),
	KW_EQUAL_MINUS("-="),
	KW_EQUAL_MUL("*="),
	KW_EQUAL_DIV("/="),
	KW_EQUAL_MOD("%="),
	
	/** line break ';', stripped by the Tokenizer to split individual lines */
	TK_LINE_BREAK(";"),
	/** new line '\n', stripped by the Tokenizer, they are completely ignored during code generation */
	TK_NL("\n"),
	TK_SPACE(" "),
	TK_DOUBLE_DOT(".."),
	TK_DOT("."),
	TK_COMMA(","),
	TK_COLUMN(":"),
	TK_BRACE_OPEN("{"), TK_BRACE_CLOSE("}"),
	TK_BRACKET_OPEN("["), TK_BRACKET_CLOSE("]"),
	TK_PARENTHESIS_OPEN("("), TK_PARENTHESIS_CLOSE(")"),
	TK_COMMENT_OPEN("/*"), TK_COMMENT_CLOSE("*/"),
	TK_LINE_COMMENT("//"),
	TK_DOUBLE_QUOTE("\""),
	TK_APOSTROPHE("'"),
	TK_BACK_APOSTROPHE("`"),
	TK_GENERIC_BINDING_BEGIN("<["),
	TK_GENERIC_BINDING_END("]>"),
	
	/** not to mistake with {@link #KW_EQUAL}, this one is <code>==</code> */
	OP_EQUALS("=="),
	OP_SEQUALS("==="),
	OP_LEQUALS("<="),
	OP_GEQUALS(">="),
	OP_NEQUALS("!="),
	OP_PLUS("+"),
	OP_MINUS("-"),
	OP_MUL("*"),
	OP_DIV("/"),
	OP_GREATER(">"),
	OP_LOWER("<"),
	OP_MOD("%"),
	OP_NOT("!"),
	OP_POWER("^"),
	OP_SHR(">>"),
	OP_SHL("<<"),
	
	OP_DIRECT_PLUS("++"),
	OP_DIRECT_MINUS("--"),
	
	;
	
	/*
	 * Some bases have multiple uses, to clarify when that happens we create aliases
	 * instead of duplicate bases that would mess up the tokenizer.
	 */
	
	// TODO replace VAR_UNIT by VAR_STRUCT everywhere it makes sense
	public static final TokenBase VAR_STRUCT = VAR_UNIT;
	
	public final String syntax;
	
	private TokenBase(String syntax) {
		this.syntax = syntax;
	}
	
}
