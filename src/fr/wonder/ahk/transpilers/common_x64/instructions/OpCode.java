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
	/** negates an int */
	NEG,
	
	/** FPU: load float on top of the fpu stack */
	FLD,
	/** FPU: converts and load an int on top of the fpu stack */
	FILD,
	/** FPU: converts the top of the fpu stack to an int and pop it to memory*/
	FISTP,
	/** FPU: pops the top of the fpu stack to memory */
	FSTP,
	/** FPU: add the value 1 on the stack */
	FLD1,
	/** FPU: add st(0) and st(1), store to st(1) and pop */
	FADDP,
	/** FPU: substract st(1) from st(0), store to st(1) and pop */
	FSUBP,
	
}
