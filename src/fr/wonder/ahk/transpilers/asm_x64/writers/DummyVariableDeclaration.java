package fr.wonder.ahk.transpilers.asm_x64.writers;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiler.Invalids;

/**
 * Used to represent the step and max values for RangedFor statements, to have
 * something to push on the MemoryManager's stack
 */
public class DummyVariableDeclaration extends VariableDeclaration {

	/** @param name solely for debug purpose */
	public DummyVariableDeclaration(String name) {
		super(Invalids.UNIT, Invalids.SOURCE_REF, name, Invalids.TYPE,
				Invalids.MODIFIERS, Invalids.EXPRESSION);
		setSignature(new Signature("Dummy", name, "Dummy-"+name));
	}

}
