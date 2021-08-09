package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;

public class StructPrototype implements Prototype<StructPrototype> {

	public final Signature signature;
	public final VariablePrototype[] members;
	public final ConstructorPrototype[] constructors;
	public final DeclarationModifiers modifiers;
	
	public StructPrototype(
			VariablePrototype[] members,
			ConstructorPrototype[] constructors,
			DeclarationModifiers modifiers,
			Signature signature) {
		this.members = members;
		this.constructors = constructors;
		this.modifiers = modifiers;
		this.signature = signature;
	}
	
	public VariablePrototype getMember(String name) {
		for(VariablePrototype mem : members)
			if(mem.getName().equals(name))
				return mem;
		return null;
	}

	@Override
	public Signature getSignature() {
		return signature;
	}
	
	/** Returns the structure name */
	public String getName() {
		return signature.name;
	}

}
