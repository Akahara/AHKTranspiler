package fr.wonder.ahk.compiled.units.prototypes;

import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiled.units.sections.StructSection;

public class StructPrototype implements Prototype<StructSection>, VarAccess {

	private final Signature signature;
	private final DeclarationModifiers modifiers;
	public final VariablePrototype[] members;
	public final ConstructorPrototype[] constructors;
	private final DeclarationVisibility visibility;
	
	public StructPrototype(DeclarationModifiers modifiers, DeclarationVisibility visibility,
			VariablePrototype[] members, ConstructorPrototype[] constructors, Signature signature) {
		this.signature = signature;
		this.modifiers = modifiers;
		this.members = members;
		this.constructors = constructors;
		this.visibility = visibility;
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

	@Override
	public VarType getType() {
		throw new IllegalStateException("Structures cannot be accessed");
	}

	@Override
	public boolean matchesDeclaration(ValueDeclaration decl) {
		return decl instanceof StructSection && ((StructSection) decl).name.equals(getName());
		// TODO0 check if more fields need to be checked to match a struct declaration
	}

	public DeclarationVisibility getVisibility() {
		return visibility;
	}

}
