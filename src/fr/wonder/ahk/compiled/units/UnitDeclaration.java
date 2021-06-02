package fr.wonder.ahk.compiled.units;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.expressions.types.VarType;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;

public class UnitDeclaration implements ValueDeclaration {
	
	public final UnitSource source;
	public final int sourceStart, sourceStop;
	public final String fullBase;

	public UnitDeclaration(UnitSource source, int sourceStart, int sourceStop, String fullBase) {
		this.source = source;
		this.sourceStart = sourceStart;
		this.sourceStop = sourceStop;
		this.fullBase = fullBase;
	}

	@Override
	public String getName() {
		return fullBase;
	}

	@Override
	public UnitSource getSource() {
		return source;
	}

	@Override
	public int getSourceStart() {
		return sourceStart;
	}

	@Override
	public int getSourceStop() {
		return sourceStop;
	}

	@Override
	public DeclarationModifiers getModifiers() {
		return DeclarationModifiers.NONE;
	}

	@Override
	public DeclarationVisibility getVisibility() {
		throw new IllegalAccessError("Unit declarations have no visibility");
	}

	@Override
	public VarType getType() {
		throw new IllegalAccessError("Unit declarations have no type");
	}
	
	@Override
	public Signature getSignature() {
		throw new IllegalAccessError("Unit declarations have no signature");
	}

}
