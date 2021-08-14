package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.commons.annotations.Nullable;

public class VariableDeclaration extends Statement {
	
	public final Unit unit;
	public final String name;
	private final VarType type;
	public final DeclarationModifiers modifiers;
	
	private VariablePrototype prototype;
	
	public VariableDeclaration(Unit unit, int sourceStart, int sourceStop,
			String name, VarType type, DeclarationModifiers modifiers, Expression defaultValue) {
		super(unit.source, sourceStart, sourceStop, defaultValue);
		this.unit = unit;
		this.name = name;
		this.type = type;
		this.modifiers = modifiers;
	}

	/** Called by the linker after types where computed */
	public void setSignature(Signature signature) {
		this.prototype = new VariablePrototype(signature, type, modifiers);
	}
	
	public Signature getSignature() {
		return prototype.getSignature();
	}
	
	public VariablePrototype getPrototype() {
		if(prototype == null)
			throw new IllegalStateException("This variable prototype was not built yet");
		return prototype;
	}
	
	@Nullable
	public Expression getDefaultValue() {
		return expressions[0];
	}
	
	@Override
	public String toString() {
		return type + " " + name + " = " + getDefaultValue();
	}
	
	public VarType getType() {
		return type;
	}
	
	@Override
	public boolean equals(Object other) {
		if(!(other instanceof VariableDeclaration))
			return false;
		VariableDeclaration o = (VariableDeclaration) other;
		return o.name.equals(name) && o.type.equals(type) && o.modifiers.visibility == modifiers.visibility;
	}
	
}
