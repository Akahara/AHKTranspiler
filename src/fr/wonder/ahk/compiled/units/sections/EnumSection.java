package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.EnumPrototype;

public class EnumSection implements SourceElement {
	
	public final SourceReference sourceRef;
	public final Unit unit;
	public final String name;
	public final DeclarationModifiers modifiers;

	// set by the enum section parser
	public String[] values;
	
	private EnumPrototype prototype;

	public EnumSection(SourceReference sourceRef, Unit unit, String name, DeclarationModifiers modifiers) {
		this.sourceRef = sourceRef;
		this.unit = unit;
		this.name = name;
		this.modifiers = modifiers;
	}

	@Override
	public SourceReference getSourceReference() {
		return sourceRef;
	}
	
	public void setSignature(Signature signature) {
		this.prototype = new EnumPrototype(name, values, modifiers, signature);
	}
	
	public EnumPrototype getPrototype() {
		return prototype;
	}
	
}
