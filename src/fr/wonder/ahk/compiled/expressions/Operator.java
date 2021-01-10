package fr.wonder.ahk.compiled.expressions;

public enum Operator {
	
	ADD			(10, true),
	SUBSTRACT	(10, true),
	MULTIPLY	(20, false),
	DIVIDE		(20, false),
	MOD			(30, false),
	
	EQUALS		(50, false),
	GREATER		(50, false),
	LOWER		(50, false),
	GEQUALS		(50, false),
	LEQUALS		(50, false),
	SEQUALS		(60, false),
	
	;
	
	public final int priority;
	public final boolean doesSingleOperand;
	
	private Operator(int priority, boolean doesSingleOperand) {
		this.priority = priority;
		this.doesSingleOperand = doesSingleOperand;
	}
	
}
