package fr.wonder.ahk.transpilers.asm_x64.units;

import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.Prototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.prototypes.blueprints.BlueprintPrototype;
import fr.wonder.ahk.transpilers.common_x64.MemSize;

/**
 * Defines the in-memory layout of GIPs.
 * <p>
 * To access any member of a structure instance that is only known through its
 * blueprints, the GIP must be queried. To do so every GIP follows the same
 * layout defined here:
 * 
 * <pre><blockquote>
 *    Member   | Offset |        Value
 * ============|========|=====================
 *  Function 1 |   0    | address of function
 *  Function 2 |   1    | 
 * ------------|--------|---------------------
 *  Variable 1 |   2    | offset of variable
 *  Variable 2 |   3    | in structure
 * ------------|--------|---------------------
 *  Operator 1 |   4    | address of the
 *  Operator 2 |   5    | operator's function
 * </blockquote></pre>
 * 
 * <p>
 * Methods in this class return offsets in the GIP, not memory offset. To get
 * the real memory offset from the GIP's pointer simply multiply an offset from
 * this class by {@link MemSize#POINTER_SIZE}.
 */
public class BlueprintLayout {

	private static int getFunctionsOffset(BlueprintPrototype bp) {
		return 0;
	}
	
	private static int getVariablesOffset(BlueprintPrototype bp) {
		return bp.functions.length;
	}
	
	private static int getOperatorsOffset(BlueprintPrototype bp) {
		return bp.functions.length + bp.variables.length;
	}
	
	private static <T extends Prototype<T>> int getIndex(T[] members, T member) {
		for(int i = 0; i < members.length; i++) {
			if(members[i].matchesPrototype(member))
				return i;
		}
		throw new IllegalArgumentException("Unknown member " + member);
	}
	
	public static int getFunctionOffset(BlueprintPrototype bp, FunctionPrototype function) {
		return getFunctionsOffset(bp) + getIndex(bp.functions, function);
	}
	
	public static int getVariableOffset(BlueprintPrototype bp, VariablePrototype variable) {
		return getVariablesOffset(bp) + getIndex(bp.variables, variable);
	}
	
	public static int getOperatorOffset(BlueprintPrototype bp, OverloadedOperatorPrototype operator) {
		return getOperatorsOffset(bp) + getIndex(bp.operators, operator);
	}
	
}
