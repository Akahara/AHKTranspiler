package fr.wonder.ahk.transpilers.asm_x64.writers;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiler.Invalids;

public class DummyVariableDeclaration extends VariableDeclaration {

	public DummyVariableDeclaration(String name) {
		super(Invalids.SOURCE, 0, 0, name, Invalids.TYPE, Invalids.EXPRESSION);
		setSignature(new Signature("Dummy", name, "Dummy-"+name));
	}

}
