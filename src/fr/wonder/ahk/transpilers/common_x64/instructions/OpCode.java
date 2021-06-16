package fr.wonder.ahk.transpilers.common_x64.instructions;

public enum OpCode {
	
	MOV,
	PUSH,
	POP,
	
	CALL,
	RET,
	JMP,
	
	XOR,
	ADD,
	SUB,
	IDIV,
	IMUL,
	DIV,
	
	INC, DEC,
	SHL, SHR,
	
	CMP,
	TEST,
	JL, JLE, // lower
	JG, JGE, // greater
	JZ, JNZ, // zero
	JE, JNE, // equal
	JS, JNS, // signed
	
	/**
	 * convert quadword to octoword, used in divisions
	 * to extend signed values in rax to rdx:rax
	 */
	CQO,
	/** negates a number */
	NEG
	
}
