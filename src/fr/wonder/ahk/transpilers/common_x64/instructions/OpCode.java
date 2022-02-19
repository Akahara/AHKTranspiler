package fr.wonder.ahk.transpilers.common_x64.instructions;

public enum OpCode {
	
	MOV,
	PUSH,
	POP,
	LEA,
	
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
	
	AND,
	OR,
	
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
	FISTTP,
	/** FPU: pops the top of the fpu stack to memory */
	FSTP,
	/** FPU: add the value 1 on the stack */
	FLD1,
	/** FPU: add st(0) and st(1), store to st(1) and pop */
	FADDP,
	/** FPU: substract st(1) from st(0), store to st(1) and pop */
	FSUBP,
	/** FPU: multiply st(0) and st(1), store to st(1) and pop */
	FMULP,
	/** FPU: divide st(0) by st(1), store to st(1) and pop */
	FDIVP,
	/** FPU: stores the remainder of st(0)/st(1) in st(0) */
	FPREM,
	/** FPU: multiply st(0) and st(1), store to st(0) */
	FMUL,
	/** FPU: compare st(0) and st(1) and pop, modify EFLAGS (ZP PF CF)  */
	FUCOMIP,
	/** FPU: exchange st(0) and st(1) */
	FXCH,
	
	/* set-byte-if-<condition> */
	SETE, SETNE, SETNZ,
	SETL, SETLE, SETG, SETGE,
	SETB, SETBE, SETA, SETAE,
	
	/** clear direction flag */
	CLD,
	/** repeat next string instruction */
	REP,
	/** string operation: move byte */
	MOVSB,
	/** string operation: store qword (copy rax to rdi and move rdi according to the direction flag) */
	STOSQ,
	
	/** loop, decrement rcx and jumps to the provided label if zero */
	LOOP,
	
}
