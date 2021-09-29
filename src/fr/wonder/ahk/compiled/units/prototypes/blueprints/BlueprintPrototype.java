package fr.wonder.ahk.compiled.units.prototypes.blueprints;

import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.Prototype;
import fr.wonder.ahk.compiled.units.prototypes.TypeAccess;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
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
	
	/**
	 * Returns a unique identifier for the given blueprint member.
	 * <p>
	 * {@code member} must be a member of this blueprint.
	 * <p>
	 * The returned id corresponds to the location in this blueprint
	 * of the member, it can safely be used to distinguish different
	 * types of prototypes (a variable and a function cannot have the
	 * same id). It may be useful in conjunction with GIPs to store
	 * and retrieve each binding in an array.
	 * <pre><blockquote>
	 * Prototype  | Id
	 * (member)   | 
	 * ===========|===
	 * Function 1 | 0
	 * Function 2 | 1
	 * -----------|---
	 * Variable 1 | 2
	 * Variable 2 | 3
	 * -----------|---
	 * Operator 1 | 4
	 * Operator 2 | 5
	 * -----------|---
	 * </blockquote></pre>
	 */
	public int getUniqueIdOfPrototype(Prototype<?> member) {
		int index = 0;
		if(member instanceof FunctionPrototype)
			return index + searchUniqueIdDisplacement(functions, (FunctionPrototype) member);
		index += functions.length;
		if(member instanceof VariablePrototype)
			return index + searchUniqueIdDisplacement(variables, (VariablePrototype) member);
		index += variables.length;
		if(member instanceof OverloadedOperatorPrototype)
			return index + searchUniqueIdDisplacement(operators, (OverloadedOperatorPrototype) member);
		throw new IllegalArgumentException("Prototype " + member + " cannot be a blueprint member");
	}
	
	private <T extends Prototype<T>> int searchUniqueIdDisplacement(T[] members, T member) {
		for(int i = 0; i < members.length; i++) {
			if(members[i].matchesPrototype(member))
				return i;
		}
		throw new IllegalArgumentException("Unknown member " + member);
	}
	
	@Override
	public Signature getSignature() {
		return signature;
	}
	
	@Override
	public DeclarationModifiers getModifiers() {
		return modifiers;
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
}
