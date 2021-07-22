package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.SourceObject;
import fr.wonder.ahk.compiled.units.prototypes.ConstructorPrototype;
import fr.wonder.ahk.compiled.units.prototypes.StructPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.commons.exceptions.UnimplementedException;
import fr.wonder.commons.utils.ArrayOperator;

public class StructSection extends SourceObject implements ValueDeclaration {

	public final String name;
	private final DeclarationModifiers modifiers;
	
	public final VariableDeclaration[] members;
	public final StructConstructor[] constructors;
	public final ConstructorDefaultValue[] nullFields;
	
	private StructPrototype prototype;
	
	public StructSection(UnitSource source, int sourceStart, int sourceStop,
			String structName, DeclarationModifiers modifiers,
			VariableDeclaration[] members, StructConstructor[] constructors,
			ConstructorDefaultValue[] nullFields) {
		super(source, sourceStart, sourceStop);
		this.name = structName;
		this.modifiers = modifiers;
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
//		if(!(other instanceof StructSection))
//			return false;
//		StructSection o = (StructSection) other;
//		if(!o.name.equals(name))
//			return false;
//		if(members.length != o.members.length)
//			return false;
//		for(VariableDeclaration member : members) {
//			if(!member.equals(o.getMember(member.name)))
//				return false;
//		}
//		// FIX check for constructors in struct equality
//		if(!modifiers.equals(o.modifiers))
//			return false;
//		return true;
	}
	
	@Override
	public String toString() {
		return "struct " + name;
	}

	@Override
	public DeclarationModifiers getModifiers() {
		return modifiers;
	}

	@Override
	public DeclarationVisibility getVisibility() {
		return DeclarationVisibility.GLOBAL; // TODO read struct visibility
	}

	@Override
	public String getName() {
		return name;
	}
	
	public void setSignature(Signature signature) {
		this.prototype = new StructPrototype(
				modifiers,
				getVisibility(),
				ArrayOperator.map(members, VariablePrototype[]::new, VariableDeclaration::getPrototype),
				ArrayOperator.map(constructors, ConstructorPrototype[]::new, StructConstructor::getPrototype),
				signature);
	}
	
	public StructPrototype getPrototype() {
		if(prototype == null)
			throw new IllegalStateException("This struct's prototype was not set yet");
		return prototype;
	}

	@Override
	public Signature getSignature() {
		return prototype.getSignature();
	}

	@Override
	public VarType getType() {
		throw new IllegalStateException("Struct sections have no type");
	}
	
}
