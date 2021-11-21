package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.units.prototypes.blueprints.BlueprintImplementation;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.GenericContext;

public class BoundStructPrototype extends StructPrototype {

	public BoundStructPrototype(
			VariablePrototype[] members,
			ConstructorPrototype[] constructors,
			OverloadedOperatorPrototype[] overloadedOperators,
			GenericContext genericContext,
			BlueprintImplementation[] implementedBlueprints,
			DeclarationModifiers modifiers,
			StructPrototype parentPrototype) {
		super(	members,
				constructors,
				overloadedOperators,
				genericContext,
				implementedBlueprints,
				modifiers,
				parentPrototype.signature);
	}
	
}