package fr.wonder.ahk.transpilers.common_x64;

import fr.wonder.ahk.transpilers.asm_x64.writers.UnitWriter;
import fr.wonder.ahk.transpilers.common_x64.addresses.LabelAddress;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;

/**
 * These labels are not included (using 'extern') by default.
 * When used, the caller must wrap the string or mem address
 * using {@link UnitWriter#requireExternLabel(String)} which
 * will add the corresponding 'extern' instruction.
 */
public class GlobalLabels {

	/** The memory location used to transfer values to the FPU */
	public static final MemAddress ADDRESS_FLOATST = new MemAddress(new LabelAddress("global_floatst"));
	/** The memory location used to transfer values to the FPU */
	public static final MemAddress ADDRESS_FSIGNBIT = new MemAddress(new LabelAddress("global_fsignbit"));
	/** The empty memory block, used for null/empty array instances */
	public static final String GLOBAL_EMPTY_MEM_BLOCK = "global_empty_mem_block";
	/** A writable but not readable qword, exists to write data to nowhere */
	public static final String GLOBAL_VOID = "global_void";
	
	/** The special alloc method label */
	public static final String SPECIAL_ALLOC = "mem_alloc_block";
	/** The special throw method label */
	public static final String SPECIAL_THROW = "error_throw";
	
	/** See closures.fasm */
	public static final String
		CLOSURE_RUN_COMPOSED = "closure_run_composed",
		CLOSURE_RUN_CONSTANT = "closure_run_constant",
		CLOSURE_RUN_OPERATION_1 = "closure_run_operation_1",
		CLOSURE_RUN_OPERATION_2 = "closure_run_operation_2",
		CLOSURE_RUN_CASTED = "closure_run_casted";
	
	/** a noop function, pops 0 arguments from the stack, keeps rax the same */
	public static final String FUNCTION_NOOP = "function_noop";
	
	/** Standard library */ // TODO handle the standard library in a different way/class
	public static final String AHK_STRINGS_INT2STR   = "ahk_Strings_int2str";
	public static final String AHK_STRINGS_FLOAT2STR = "ahk_Strings_float2str";
	public static final String AHK_STRINGS_BOOL2STR  = "ahk_Strings_bool2str";
	
	
}
