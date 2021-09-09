package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.GenericContext;

public class BoundStructPrototype extends StructPrototype {

	public BoundStructPrototype(
			VariablePrototype[] members,
			ConstructorPrototype[] constructors,
			OverloadedOperatorPrototype[] overloadedOperators,
			GenericContext genericContext,
			DeclarationModifiers modifiers,
			StructPrototype parentPrototype) {
		super(	members,
				constructors,
				overloadedOperators,
				genericContext,
				modifiers,
				parentPrototype.signature);
	}
	
	
	
}
