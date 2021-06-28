package fr.wonder.ahk.transpilers.common_x64;

import fr.wonder.ahk.transpilers.common_x64.addresses.LabelAddress;
import fr.wonder.ahk.transpilers.common_x64.addresses.MemAddress;

public class GlobalLabels {

	/** The memory location that can be used to transfer float values to the FPU */
	public static final String GLOBAL_FLOATST = "global_floatst";
	public static final MemAddress ADDRESS_FLOATST = new MemAddress(new LabelAddress(GLOBAL_FLOATST));
	public static final String GLOBAL_VAL_FSIGNBIT = "global_val_fsignbit";
	public static final MemAddress ADDRESS_VAL_FSIGNBIT = new MemAddress(new LabelAddress(GLOBAL_VAL_FSIGNBIT));
	
	/** The special alloc method label */
	public static final String SPECIAL_ALLOC = "mem_alloc_block";
	/** The special throw method label */
	public static final String SPECIAL_THROW = "error_throw";

}
