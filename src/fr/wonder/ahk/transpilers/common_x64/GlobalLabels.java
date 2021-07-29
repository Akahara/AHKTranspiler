package fr.wonder.ahk.transpilers.common_x64;

import fr.wonder.ahk.transpilers.common_x64.addresses.LabelAddress;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;

public class GlobalLabels {

	/** The memory location used to transfer values to the FPU */
	public static final String GLOBAL_FLOATST = "global_floatst";
	/** The memory location used to transfer values to the FPU */
	public static final MemAddress ADDRESS_FLOATST = new MemAddress(new LabelAddress(GLOBAL_FLOATST));
	/** The float sign bit mask, used in {@link #ADDRESS_VAL_FSIGNBIT} */
	public static final String GLOBAL_VAL_FSIGNBIT = "global_val_fsignbit";
	/** The float sign bit mask, used by the Operation Writer to negate a float without passing it to the fpu */
	public static final MemAddress ADDRESS_VAL_FSIGNBIT = new MemAddress(new LabelAddress(GLOBAL_VAL_FSIGNBIT));
	/** The empty memory block, used for null/empty array instances */
	public static final String GLOBAL_EMPTY_MEM_BLOCK = "global_empty_mem_block";
	
	/** The special alloc method label */
	public static final String SPECIAL_ALLOC = "mem_alloc_block";
	/** The special throw method label */
	public static final String SPECIAL_THROW = "error_throw";

}
