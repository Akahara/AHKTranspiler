package fr.wonder.ahk.compiler;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.UnitImportation;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;

public class Unit {
	
	public final UnitSource source;
	
	public String base;
	public String name;
	
	public UnitImportation[] importations;
	public VariableDeclaration[] variables;
	public FunctionSection[] functions;
	
	public Unit(UnitSource source) {
		this.source = source;
	}

	public String getFullBase() {
		return base+'.'+name;
	}
	
	@Override
	public String toString() {
		return getFullBase();
	}
	
}
