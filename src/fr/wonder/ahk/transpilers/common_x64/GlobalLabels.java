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
	
	/** The special alloc method label */
	public static final String SPECIAL_ALLOC = "mem_alloc_block";
	/** The special throw method label */
	public static final String SPECIAL_THROW = "error_throw";
	
	/** TODO0 comment closure_run_composed */
	public static final String CLOSURE_RUN_COMPOSED_FUNC = "closure_run_composed";
	public static final String CLOSURE_RUN_CONSTANT_FUNC = "closure_run_constant";
	
}
