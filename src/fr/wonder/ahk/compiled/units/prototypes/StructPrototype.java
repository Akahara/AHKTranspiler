package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.StructSection;

public class StructPrototype implements Prototype<StructSection> {

	private final Signature signature;
	private final DeclarationModifiers modifiers;
	public final VariablePrototype[] members;
	public final ConstructorPrototype[] constructors;
	
	public StructPrototype(DeclarationModifiers modifiers, VariablePrototype[] members, ConstructorPrototype[] constructors, Signature signature) {
		this.signature = signature;
		this.modifiers = modifiers;
		this.members = members;
		this.constructors = constructors;
	}
	
	public VariablePrototype getMember(String name) {
		for(VariablePrototype mem : members)
			if(mem.getName().equals(name))
				return mem;
		return null;
	}
	
	@Override
	public DeclarationModifiers getModifiers() {
		return modifiers;
	}

	@Override
	public Signature getSignature() {
		return signature;
	}
	
	/** Returns the structure name */
	public String getName() {
		return signature.name;
	}

	@Override
	public StructSection getAccess(Unit unit) {
		throw new IllegalStateException("Structs cannot be accessed");
	}

}
