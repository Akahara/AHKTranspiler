package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.prototypes.blueprints.BlueprintPrototype;
import fr.wonder.commons.utils.ArrayOperator;

public class Blueprint implements SourceElement {
	
	public final SourceReference sourceRef;
	
	public final Unit unit;
	public final String name;
	public final DeclarationModifiers modifiers;
	
	public VariableDeclaration[] variables;
	public FunctionSection[] functions;
	public BlueprintOperator[] operators;
	
	private BlueprintPrototype prototype;
	
	public Blueprint(Unit unit, String name, DeclarationModifiers modifiers, SourceReference sourceRef) {
		this.unit = unit;
		this.name = name;
		this.modifiers = modifiers;
		this.sourceRef = sourceRef;
	}

	@Override
	public SourceReference getSourceReference() {
		return sourceRef;
	}
	
	public void setSignature(Signature signature) {
		this.prototype = new BlueprintPrototype(
				ArrayOperator.map(functions, FunctionPrototype[]::new, FunctionSection::getPrototype),
				ArrayOperator.map(variables, VariablePrototype[]::new, VariableDeclaration::getPrototype),
				ArrayOperator.map(operators, OverloadedOperatorPrototype[]::new, BlueprintOperator::getPrototype),
				modifiers,
				signature);
	}
	
	public BlueprintPrototype getPrototype() {
		return prototype;
	}
	
}
