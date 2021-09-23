package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;

public interface TypeAccess {
	
	public Signature getSignature();
	public DeclarationModifiers getModifiers();
	
}
