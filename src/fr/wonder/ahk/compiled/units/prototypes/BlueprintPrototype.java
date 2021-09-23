package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;

public class BlueprintPrototype implements Prototype<BlueprintPrototype>, TypeAccess {
	
	public final FunctionPrototype[] functions;
	public final VariablePrototype[] variables;
	public final OverloadedOperatorPrototype[] operators;
	public final DeclarationModifiers modifiers;
	
	public final Signature signature;
	
	public BlueprintPrototype(FunctionPrototype[] functions, VariablePrototype[] variables,
			OverloadedOperatorPrototype[] operators, DeclarationModifiers modifiers, Signature signature) {
		this.functions = functions;
		this.variables = variables;
		this.operators = operators;
		this.modifiers = modifiers;
		this.signature = signature;
	}
	
	@Override
	public Signature getSignature() {
		return signature;
	}
	
	@Override
	public DeclarationModifiers getModifiers() {
		return modifiers;
	}
	
}
