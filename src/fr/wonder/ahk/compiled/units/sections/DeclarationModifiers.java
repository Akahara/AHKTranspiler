package fr.wonder.ahk.compiled.units.sections;

import java.util.Arrays;

import fr.wonder.commons.utils.ArrayOperator;

public class DeclarationModifiers {
	
	public static final DeclarationModifiers NONE = new DeclarationModifiers(DeclarationVisibility.LOCAL, new Modifier[0]);
	
	private final Modifier[] modifiers;
	private boolean validSyntaxes = false;
	
	public final DeclarationVisibility visibility;
	
	public DeclarationModifiers(DeclarationVisibility visibility, Modifier[] modifiers) {
		this.visibility = visibility;
		this.modifiers = modifiers;
	}
	
	public Modifier getModifier(String name) {
		for(int i = 0; i < modifiers.length; i++) {
			if(modifiers[i].name.equals(name))
				return modifiers[i];
		}
		return null;
	}
	
	private void assertValidSyntaxes() {
		if(validSyntaxes) return;
		for(Modifier m : modifiers)
			if(m.syntax == null)
				throw new IllegalStateException("Modifier " + m + " has no syntax");
		validSyntaxes = true;
	}
	
	public <T extends ModifierSyntax> T getModifier(Class<T> type) {
		assertValidSyntaxes();
		for(Modifier m : modifiers) {
			if(m.syntax.getClass() == type)
				return type.cast(m.syntax);
		}
		return null;
	}
	
	public boolean hasModifier(String name) {
		return getModifier(name) != null;
	}
	
	@Override
	public int hashCode() {
		return modifiers.hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if(!(other instanceof DeclarationModifiers))
			return false;
		DeclarationModifiers o = (DeclarationModifiers) other;
		if(o.modifiers.length != modifiers.length)
			return false;
		for(Modifier m : modifiers) {
			if(!m.equals(o.getModifier(m.name)))
				return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return Arrays.toString(ArrayOperator.map(modifiers, String[]::new, m->m.name));
	}
	
}
