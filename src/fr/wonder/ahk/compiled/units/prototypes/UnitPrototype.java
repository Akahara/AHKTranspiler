package fr.wonder.ahk.compiled.units.prototypes;

import java.util.HashSet;
import java.util.Set;

import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.commons.utils.ArrayOperator;

public class UnitPrototype implements Prototype<UnitPrototype> {
	
	public static final UnitPrototype NULL_PROTOTYPE = 
			new UnitPrototype("NULL", new String[0], new FunctionPrototype[0], new VariablePrototype[0], new StructPrototype[0]);
	
	public final String base;
	public final String fullBase;
	public final String[] importations;
	
	public final FunctionPrototype[] functions;
	public final VariablePrototype[] variables;
	public final StructPrototype[] structures;
	
	public final Signature signature;
	
	/**
	 * The list of variables and functions used but not contained by this unit.
	 * This list may not contain {@link FunctionArgument}s.
	 */
	public Set<Prototype<?>> externalAccesses = new HashSet<>();
	
	public UnitPrototype(String fullBase, String[] importations,
			FunctionPrototype[] functions, VariablePrototype[] variables, StructPrototype[] structures) {
		this.base = fullBase.substring(fullBase.lastIndexOf('.')+1);
		this.fullBase = fullBase;
		this.importations = importations;
		this.functions = functions;
		this.variables = variables;
		this.structures = structures;
		this.signature = new Signature(fullBase, base, fullBase);
	}
	
	@Override
	public boolean matchesPrototype(UnitPrototype other) {
		UnitPrototype p = (UnitPrototype) other;
		if(!fullBase.equals(p.fullBase))
			return false;
		if(functions.length != p.functions.length || variables.length != p.variables.length)
			return false;
		for(int i = 0; i < functions.length; i++) {
			if(!functions[i].matchesPrototype(p.functions[i]))
				return false;
		}
		for(int i = 0; i < variables.length; i++) {
			if(!variables[i].matchesPrototype(p.variables[i]))
				return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return fullBase;
	}
	
	public UnitPrototype[] filterImportedUnits(UnitPrototype[] prototypes) {
		return ArrayOperator.filter(prototypes, proto -> ArrayOperator.contains(importations, proto.fullBase));
	}
	
	public VariablePrototype getVariable(String name) {
		for(VariablePrototype var : variables) {
			if(var.getName().equals(name))
				return var;
		}
		return null;
	}
	
	public FunctionPrototype getFunction(String name) {
		for(FunctionPrototype func : functions) {
			if(func.getName().equals(name))
				return func;
		}
		return null;
	}

	@Override
	public Signature getSignature() {
		return signature;
	}
	
}
