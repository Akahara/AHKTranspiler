package fr.wonder.ahk.compiler;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;

public class Unit {
	
	public final UnitSource source;
	
	public final String base;
	public final String name;
	public final String fullBase;
	public final String[] importations;
	
	public final VariableDeclaration[] variables;
	public final FunctionSection[] functions;
	
	public UnitPrototype prototype;
	
	public Unit(UnitSource source, String base, String name, String[] importations,
			VariableDeclaration[] variables, FunctionSection[] functions) {
		this.source = source;
		this.base = base;
		this.name = name;
		this.fullBase = base+'.'+name;
		this.importations = importations;
		this.variables = variables;
		this.functions = functions;
	}
	
	@Override
	public String toString() {
		return fullBase;
	}
	
}
