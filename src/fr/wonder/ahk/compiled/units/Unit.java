package fr.wonder.ahk.compiled.units;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.expressions.types.VarStructType;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.linker.Prototypes;
import fr.wonder.commons.types.Triplet;

public class Unit {
	
	public final UnitSource source;
	
	public final UnitDeclaration declaration;
	public final String base;
	public final String name;
	public final String fullBase;
	public final String[] importations;
	
	public final VariableDeclaration[] variables;
	public final FunctionSection[] functions;
	public final StructSection[] structures;
	
	public final List<Triplet<VarStructType, SourceElement, Integer>> usedStructTypes = new ArrayList<>();
	
	/** Set by {@link Prototypes#buildPrototype(Unit)} */
	public UnitPrototype prototype;
	
	public Unit(UnitSource source, String base, String name, int declarationStart, int declarationStop,
			String[] importations, VariableDeclaration[] variables, FunctionSection[] functions, StructSection[] structures) {
		this.declaration = new UnitDeclaration(source, declarationStart, declarationStop, base);
		this.source = source;
		this.base = base;
		this.name = name;
		this.fullBase = base+'.'+name;
		this.importations = importations;
		this.variables = variables;
		this.functions = functions;
		this.structures = structures;
	}
	
	@Override
	public String toString() {
		return fullBase;
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof Unit && fullBase.equals(((Unit) other).fullBase);
	}
	
}
