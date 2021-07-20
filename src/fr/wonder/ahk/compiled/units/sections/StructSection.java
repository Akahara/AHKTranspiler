package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.SourceObject;

public class StructSection extends SourceObject {

	public final String structName;
	public final DeclarationModifiers modifiers;
	
	public final VariableDeclaration[] members;
	
	public StructSection(UnitSource source, int sourceStart, int sourceStop,
			String structName, DeclarationModifiers modifiers, VariableDeclaration[] members) {
		super(source, sourceStart, sourceStop);
		this.structName = structName;
		this.modifiers = modifiers;
		this.members = members;
	}
	
	public VariableDeclaration getMember(String name) {
		for(VariableDeclaration member : members) {
			if(member.name.equals(name))
				return member;
		}
		return null;
	}

	@Override
	public boolean equals(Object other) {
		if(!(other instanceof StructSection))
			return false;
		StructSection o = (StructSection) other;
		if(!o.structName.equals(structName))
			return false;
		if(members.length != o.members.length)
			return false;
		for(VariableDeclaration member : members) {
			if(!member.equals(o.getMember(member.name)))
				return false;
		}
		if(!modifiers.equals(o.modifiers))
			return false;
		return true;
	}
	
}
