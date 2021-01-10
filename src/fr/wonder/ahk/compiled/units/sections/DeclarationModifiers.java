package fr.wonder.ahk.compiled.units.sections;

public class DeclarationModifiers {
	
	public static final DeclarationModifiers NONE = new DeclarationModifiers(new Modifier[0]);
	
	private final Modifier[] modifiers;
	private final boolean[] usedModifiers;
	
	public DeclarationModifiers(Modifier[] modifiers) {
		this.modifiers = modifiers;
		this.usedModifiers = new boolean[modifiers.length];
	}
	
	public Modifier getModifier(String name) {
		for(int i = 0; i < modifiers.length; i++) {
			if(modifiers[i].name.equals(name)) {
				usedModifiers[i] = true;
				return modifiers[i];
			}
		}
		return null;
	}
	
	public <T extends ModifierSyntax> T getModifier(Class<T> type) {
		for(Modifier m : modifiers) {
			if(m.syntax != null && m.syntax.getClass() == type)
				return type.cast(m.syntax);
		}
		return null;
	}
	
	public boolean hasModifier(String name) {
		return getModifier(name) != null;
	}
	
}
