package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;

public class VariableDeclaration extends Statement implements ValueDeclaration {
	
	public final String name;
	private final VarType type;
	public DeclarationModifiers modifiers = DeclarationModifiers.NONE;
	public DeclarationVisibility visibility = DeclarationVisibility.GLOBAL; // TODO read variable declaration visibility
	
	private VariablePrototype prototype;
	
	public VariableDeclaration(UnitSource source, int sourceStart, int sourceStop,
			String name, VarType type, Expression defaultValue) {
		super(source, sourceStart, sourceStop, defaultValue);
		this.name = name;
		this.type = type;
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
	
	public Expression getDefaultValue() {
		return expressions[0];
	}
	
	@Override
	public String toString() {
		return type + " " + name + " = " + getDefaultValue();
	}

	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public VarType getType() {
		return type;
	}

	@Override
	public DeclarationVisibility getVisibility() {
		return visibility;
	}
	
	@Override
	public DeclarationModifiers getModifiers() {
		return modifiers;
	}
	
}
