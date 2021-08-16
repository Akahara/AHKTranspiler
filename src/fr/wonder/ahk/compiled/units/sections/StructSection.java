package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceObject;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.OverloadedOperatorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.commons.exceptions.UnimplementedException;
import fr.wonder.commons.utils.ArrayOperator;

public class StructSection extends SourceObject {

	public final String name;
	public final Unit unit;
	public final DeclarationModifiers modifiers;
	
	// set by the struct section parser
	public VariableDeclaration[] members;
	public StructConstructor[] constructors;
	public ConstructorDefaultValue[] nullFields;
	public OverloadedOperator[] operators;
	
	private StructPrototype prototype;
	
	public StructSection(Unit unit, int sourceStart, int sourceStop,
			String structName, DeclarationModifiers modifiers) {
		super(unit.source, sourceStart, sourceStop);
		this.name = structName;
		this.unit = unit;
		this.modifiers = modifiers;
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
	
	/**
	 * Must be called <b>after</b> the {@code setSignature} method of all members
	 * and constructors of this structure.
	 */
	public void setSignature(Signature signature) {
		this.prototype = new StructPrototype(
				ArrayOperator.map(members, VariablePrototype[]::new, VariableDeclaration::getPrototype),
				ArrayOperator.map(constructors, ConstructorPrototype[]::new, StructConstructor::getPrototype),
				ArrayOperator.map(operators, OverloadedOperatorPrototype[]::new, OverloadedOperator::getPrototype),
				modifiers,
				signature);
	}
	
	public StructPrototype getPrototype() {
		if(prototype == null)
			throw new IllegalStateException("This struct's prototype was not set yet");
		return prototype;
	}

}
