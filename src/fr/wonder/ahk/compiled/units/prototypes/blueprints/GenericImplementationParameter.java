package fr.wonder.ahk.compiled.units.prototypes.blueprints;

import fr.wonder.ahk.compiled.expressions.types.VarGenericType;
import fr.wonder.ahk.compiled.units.sections.BlueprintRef;

/**
 * <b>GIP</b>
 * <br>
 * Generic Implementation Parameters are function parameters passed to
 * generic functions that have requirements on the types of its generic
 * arguments.
 * 
 * <p>
 * Although they do not appear as explicit arguments in the function parameters
 * they must be passed for the function to use operations declared by the
 * generic implemented blueprints.
 * 
 * <blockquote><pre>
 * func <[T, U : #Bp1, V : #Bp2 & #Bp3]> void f(T t, U u) {
 * 	...
 * }
 * </pre></blockquote>
 * 
 * <p>In this example the function {@code f} requires 3 GIPs:
 * <ul>
 * <li>None for type {@code T}, this generic does not declare a blueprint</li>
 * <li>One { U - #Bp1 }</li>
 * <li>Two { V - #Bp2 } and { V - #Bp3 }</li>
 * </ul>
 * Even though the type {@code V} does not appear in the function declaration
 * its GIPs must be passed because it may create new instances of {@code V}
 * if {@code #Bp2} or {@code #Bp3} declares a constructor.
 */
public class GenericImplementationParameter {
	
	public final VarGenericType genericType;
	public final BlueprintRef typeRequirement;
	
	public GenericImplementationParameter(VarGenericType genericType, BlueprintRef typeRequirement) {
		this.genericType = genericType;
		this.typeRequirement = typeRequirement;
	}
	
	@Override
	public String toString() {
		return genericType.toString() + ":" + typeRequirement;
	}
	
}
