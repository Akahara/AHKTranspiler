package fr.wonder.ahk.compiled.units.prototypes;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.commons.utils.ArrayOperator;

/** Dynamically linked unit */
public class UnitPrototype {
	
	public final String base;
	public final String fullBase;
	public final String[] importations;
	
	public final FunctionPrototype[] functions;
	public final VariablePrototype[] variables;
	
	public UnitPrototype(String fullBase, String[] importations,
			FunctionPrototype[] functions, VariablePrototype[] variables) {
		this.base = fullBase.substring(fullBase.lastIndexOf('.')+1);
		this.fullBase = fullBase;
		this.importations = importations;
		this.functions = functions;
		this.variables = variables;
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
	
	public FunctionPrototype[] getFunctions(String name) {
		List<FunctionPrototype> functions = new ArrayList<>();
		for(FunctionPrototype f : this.functions) {
			if(f.name.equals(name))
				functions.add(f);
		}
		return functions.toArray(FunctionPrototype[]::new);
	}
	
}
