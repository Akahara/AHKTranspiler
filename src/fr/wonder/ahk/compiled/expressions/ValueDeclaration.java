package fr.wonder.ahk.compiled.expressions;

import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;

public interface ValueDeclaration extends SourceElement {
	
	public DeclarationModifiers getModifiers();
	public DeclarationVisibility getVisibility();
	public String getName();
	public Signature getSignature();
	public VarType getType();

}
