package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.Prototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;

/**
 * The BlueprintImplementation class is the concrete link between a structure
 * declaring it implements a certain blueprint and said blueprint.
 * 
 * <p>
 * At parsing time the structure only knows the names of the blueprints it
 * implements, an instance of this class is created for each. The blueprint
 * reference is linked by the prelinker (the same mechanism is used to link
 * structure references).
 * 
 * <p>
 * The linker then fills the {@code variables}, {@code functions} and
 * {@code operators} fields of this instance with the prototypes of the parent
 * structure that match the blueprint's declarations.
 * 
 * <p>
 * A transpiler can then refer to these implementations using the corresponding
 * {@code getImplementation} method.
 * 
 * <p>
 * Note that an implementation variable/function must be declared global.
 * 
 * @implNote The {@code getImplementation} methods require the linker to list
 *           the implementation prototypes in the order their respective
 *           declarations appear in the blueprint.
 */
public class BlueprintImplementation {

	public final BlueprintRef blueprint;

	public VariablePrototype[] variables;
	public FunctionPrototype[] functions;
	public OverloadedOperatorPrototype[] operators;

	public BlueprintImplementation(BlueprintRef blueprint) {
		this.blueprint = blueprint;
	}

	public VariablePrototype getImplementation(VariablePrototype blueprintVar) {
		return getImplementation(variables, blueprint.blueprint.variables, blueprintVar);
	}

	public FunctionPrototype getImplementation(FunctionPrototype blueprintFunc) {
		return getImplementation(functions, blueprint.blueprint.functions, blueprintFunc);
	}

	public OverloadedOperatorPrototype getImplementation(OverloadedOperatorPrototype blueprintOperator) {
		return getImplementation(operators, blueprint.blueprint.operators, blueprintOperator);
	}

	private static <T extends Prototype<T>> T getImplementation(
			T[] implementations, T[] blueprintDeclarations, T searched) {

		for (int i = 0; i < blueprintDeclarations.length; i++) {
			if (blueprintDeclarations[i].matchesPrototype(searched))
				return implementations[i];
		}
		throw new IllegalArgumentException("The blueprint does not declare the researched prototype");
	}

}
