package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiler.types.FunctionArguments;

/**
 * Callable prototypes are prototypes that can be called with a number of
 * arguments of specific types, for example {@link FunctionPrototype} and
 * {@link ConstructorPrototype}. Multiple of these may have a similar signature
 * hence the need to check argument types with {@link FunctionArguments}
 */
public interface CallablePrototype {
	
	/**
	 * Returns the argument types with which this can be called.
	 */
	public VarType[] getArgumentTypes();
	
}
