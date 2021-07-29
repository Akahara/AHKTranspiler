package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;

public class AliasPrototype implements Prototype<AliasPrototype> {

	public final String text;
	public final Signature signature;
	public final VarType resolvedType;
	
	public AliasPrototype(String unitFullBase, String text, VarType resolvedType) {
		this.text = text;
		this.signature = new Signature(unitFullBase, text, "alias_"+text+"_"+resolvedType.getSignature()); // TODO rework all signatures
		this.resolvedType = resolvedType;
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
