package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceObject;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.commons.exceptions.UnimplementedException;
import fr.wonder.commons.utils.ArrayOperator;

public class StructSection extends SourceObject {

	public final String name;
	
	public final VariableDeclaration[] members;
	public final StructConstructor[] constructors;
	public final ConstructorDefaultValue[] nullFields;
	public final DeclarationVisibility visibility = DeclarationVisibility.GLOBAL;
	
	private StructPrototype prototype;
	
	public StructSection(UnitSource source, int sourceStart, int sourceStop,
			String structName, VariableDeclaration[] members, StructConstructor[] constructors,
			ConstructorDefaultValue[] nullFields) {
		super(source, sourceStart, sourceStop);
		this.name = structName;
		this.members = members;
		this.constructors = constructors;
		this.nullFields = nullFields;
	}
	
	public VariableDeclaration getMember(String name) {
		for(VariableDeclaration member : members) {
			if(member.name.equals(name))
				return member;
		}
		return null;
	}
	
	public ConstructorDefaultValue getNullField(String name) {
		for(ConstructorDefaultValue field : nullFields) {
			if(field.name.equals(name))
				return field;
		}
		return null;
	}

	@Override
	public boolean equals(Object other) {
		throw new UnimplementedException("Unimplemented equality check: cyclic structures problem");
	}
	
	@Override
	public String toString() {
		return "struct " + name;
	}

	public String getName() {
		return name;
	}
	
	public void setSignature(Signature signature) {
		this.prototype = new StructPrototype(
				visibility,
				ArrayOperator.map(members, VariablePrototype[]::new, VariableDeclaration::getPrototype),
				ArrayOperator.map(constructors, ConstructorPrototype[]::new, StructConstructor::getPrototype),
				signature);
	}
	
	public StructPrototype getPrototype() {
		if(prototype == null)
			throw new IllegalStateException("This struct's prototype was not set yet");
		return prototype;
	}

}
