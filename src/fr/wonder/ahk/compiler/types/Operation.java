package fr.wonder.ahk.compiler.types;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.commons.annotations.Nullable;

/**
 * An operation takes a right and possibly a right operand and return a value,
 * native operations are stored in the {@link NativeOperation} class and
 * user-defined operations (operator overloading) are declared in structures.
 */
public interface Operation {

	/** Returns the result type of this of this operation */
	public VarType getResultType();

	/**
	 * Returns the left operand type, if {@code null} this operation does not take a
	 * left operand
	 */
	@Nullable
	public VarType getLOType();

	/** Returns the right operand type */
	public VarType getROType();

}
