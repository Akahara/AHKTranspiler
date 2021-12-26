package fr.wonder.ahk.compiled.expressions;

public enum Operator {
	
	OR			(-30, false),
	AND			(-30, false),
	
	STRICTEQUALS(-20, false),
	
	EQUALS		(-20, false),
	GREATER		(-20, false),
	LOWER		(-20, false),
	GEQUALS		(-20, false),
	LEQUALS		(-20, false),
	NEQUALS		(-20, false),

	POWER		(-10, false),
	NOT			(-10, true),

	SHR			(0, false),
	SHL			(0, false),
	
	ADD			(10, false),
	SUBSTRACT	(10, true),
	MULTIPLY	(20, false),
	DIVIDE		(20, false),
	MOD			(30, false),

	BITWISE_OR	(40, false),
	BITWISE_AND	(40, false),
	
	;
	
	public final int priority;
	public final boolean doesSingleOperand;
	
	private Operator(int priority, boolean doesSingleOperand) {
		this.priority = priority;
		this.doesSingleOperand = doesSingleOperand;
	}
	
	public static Operator getLogicalOposite(Operator o) {
		switch(o) {
		case EQUALS:  return NEQUALS;
		case GREATER: return LEQUALS;
		case GEQUALS: return LOWER;
		case LOWER:   return GEQUALS;
		case LEQUALS: return GREATER;
		default: return null;
		}
	}
	
}
