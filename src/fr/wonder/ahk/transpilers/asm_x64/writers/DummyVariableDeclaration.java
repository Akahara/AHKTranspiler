package fr.wonder.ahk.transpilers.asm_x64.writers;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiler.Invalids;

/**
 * Used to represent the step and max values for RangedFor statements, to have
 * something to push on the MemoryManager's stack
 */
public class DummyVariableDeclaration extends VariableDeclaration {

	public DummyVariableDeclaration(String name) {
		super(Invalids.UNIT, 0, 0, name, Invalids.TYPE, Invalids.EXPRESSION);
		setSignature(new Signature("Dummy", name, "Dummy-"+name));
	}

}
