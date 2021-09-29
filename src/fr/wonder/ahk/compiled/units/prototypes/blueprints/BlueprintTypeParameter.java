package fr.wonder.ahk.compiled.units.prototypes.blueprints;

/**
 * <b>BPTP</b>
 */
public class BlueprintTypeParameter {

	public final GenericImplementationParameter functionParameter;
	public final BlueprintImplementation implementation;

	public BlueprintTypeParameter(GenericImplementationParameter functionParameter,
			BlueprintImplementation implementation) {
		this.functionParameter = functionParameter;
		this.implementation = implementation;
	}

}
