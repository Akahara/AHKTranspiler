package fr.wonder.ahk.compiled.statements;

import fr.wonder.ahk.compiled.expressions.Expression;
import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiler.Unit;

public class VariableDeclaration extends Statement implements ValueDeclaration {
	
	public final String name;
	public final VarType type;
	public DeclarationModifiers modifiers;
	public DeclarationVisibility visibility = DeclarationVisibility.GLOBAL; // TODO read variable declaration visibility
	
	public VariableDeclaration(Unit unit, int sourceStart, int sourceStop, String name, VarType type, Expression defaultValue) {
		super(unit, sourceStart, sourceStop, defaultValue);
		this.name = name;
		this.type = type;
	}
	
	public VariableDeclaration(Unit unit, int sourceStart, int sourceStop, String name, VarType type) {
		super(unit, sourceStart, sourceStop);
		this.name = name;
		this.type = type;
	}
	
	public Expression getDefaultValue() {
		return expressions.length != 0 ? expressions[0] : null;
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
		return modifiers == null ? DeclarationModifiers.NONE : modifiers;
	}
	
}
