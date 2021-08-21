package fr.wonder.ahk.compiled.expressions;

public enum Operator {
	
	POWER		(-10, false),
	NOT			(-10, true),

	SHR			(0, false),
	SHL			(0, false),
	
	ADD			(10, false),
	SUBSTRACT	(10, true),
	MULTIPLY	(20, false),
	DIVIDE		(20, false),
	MOD			(30, false),
	
	EQUALS		(50, false),
	GREATER		(50, false),
	LOWER		(50, false),
	GEQUALS		(50, false),
	LEQUALS		(50, false),
	NEQUALS		(50, false),
	/** Strict egality check '===' */
	SEQUALS		(60, false),
	
	;
	
	public final int priority;
	public final boolean doesSingleOperand;
	
	private Operator(int priority, boolean doesSingleOperand) {
		this.priority = priority;
		this.doesSingleOperand = doesSingleOperand;
	}
	
}
