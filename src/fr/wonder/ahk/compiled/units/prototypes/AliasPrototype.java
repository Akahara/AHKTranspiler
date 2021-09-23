package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiler.linker.Signatures;

public class AliasPrototype implements Prototype<AliasPrototype> {

	public final String text;
	public final Signature signature;
	public final VarType resolvedType;
	
	public AliasPrototype(String unitFullBase, String text, VarType resolvedType) {
		this.text = text;
		this.resolvedType = resolvedType;
		this.signature = Signatures.of(this, unitFullBase);
	}
	
	@Override
	public String getName() {
		return text;
	}

	@Override
	public Signature getSignature() {
		return signature;
	}
}
