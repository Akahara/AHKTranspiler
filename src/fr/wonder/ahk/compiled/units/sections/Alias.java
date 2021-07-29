package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceObject;
import fr.wonder.ahk.compiled.units.prototypes.Prototype;

public class Alias extends SourceObject implements Prototype<Alias> {
	
	public final String text;
	public final VarType resolvedType;
	public final DeclarationVisibility visibility = DeclarationVisibility.GLOBAL;
	public final Signature signature;
	
	public Alias(UnitSource source, int sourceStart, int sourceStop,
			String text, VarType resolvedType) {
		super(source, sourceStart, sourceStop);
		this.text = text;
		this.resolvedType = resolvedType;
		this.signature = new Signature("ALIAS", text, text); // TODO rework ALL Signatures
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