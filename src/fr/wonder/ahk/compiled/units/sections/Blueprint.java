package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.Unit;

public class Blueprint implements SourceElement {
	
	public final SourceReference sourceRef;
	
	public final Unit unit;
	public final String name;
	public final DeclarationModifiers modifiers;
	public final GenericContext genericContext;
	
	public VariableDeclaration[] variables;
	public FunctionSection[] functions;
	
	public Blueprint(Unit unit, String name, GenericContext genericContext,
			DeclarationModifiers modifiers, SourceReference sourceRef) {
		this.unit = unit;
		this.name = name;
		this.genericContext = genericContext;
		this.modifiers = modifiers;
		this.sourceRef = sourceRef;
	}

	@Override
	public SourceReference getSourceReference() {
		return sourceRef;
	}
	
}
