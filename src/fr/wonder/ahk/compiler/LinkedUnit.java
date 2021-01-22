package fr.wonder.ahk.compiler;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;

public class LinkedUnit { // FIX migrate LinkedUnit to fr.wonder.ahk.compiled

	public final UnitSource source;
	public final String base;
	public final String name;
	public final String fullBase;
	public LinkedUnit[] importations;
	
	public final VariableDeclaration[] variables;
	public final FunctionSection[] functions;
	
	public LinkedUnit(Unit unit) {
		this.source = unit.source;
		this.base = unit.base;
		this.name = unit.name;
		this.fullBase = unit.fullBase;
		this.variables = unit.variables;
		this.functions = unit.functions;
		this.importations = new LinkedUnit[unit.importations.length];
	}
	
	public LinkedUnit getReachableUnit(String name) {
		if(this.name.equals(name))
			return this;
		for(LinkedUnit u : importations)
			if(u.name.equals(name))
				return u;
		throw new IllegalArgumentException("Unknown unit " + name);
//		return null;
	}
	
	@Override
	public String toString() {
		return fullBase;
	}
	
}