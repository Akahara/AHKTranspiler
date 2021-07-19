package fr.wonder.ahk.compiled.units.prototypes;

import java.util.HashSet;
import java.util.Set;

import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiled.units.Signature;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.UnitDeclaration;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.FunctionArgument;
import fr.wonder.commons.utils.ArrayOperator;

public class UnitPrototype implements Prototype<UnitDeclaration> {
	
	public static final UnitPrototype NULL_PROTOTYPE = 
			new UnitPrototype("NULL", new String[0], new FunctionPrototype[0], new VariablePrototype[0]);
	
	public final String base;
	public final String fullBase;
	public final String[] importations;
	
	public final FunctionPrototype[] functions;
	public final VariablePrototype[] variables;
	
	/**
	 * The list of variables and functions used but not contained by this unit.
	 * This list may not contain {@link FunctionArgument}s.
	 */
	public Set<VarAccess> externalAccesses = new HashSet<>();
	
	public UnitPrototype(String fullBase, String[] importations,
			FunctionPrototype[] functions, VariablePrototype[] variables) {
		this.base = fullBase.substring(fullBase.lastIndexOf('.')+1);
		this.fullBase = fullBase;
		this.importations = importations;
		this.functions = functions;
		this.variables = variables;
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof UnitPrototype))
			return false;
		UnitPrototype p = (UnitPrototype) o;
		if(!fullBase.equals(p.fullBase))
			return false;
		if(functions.length != p.functions.length || variables.length != p.variables.length)
			return false;
		for(int i = 0; i < functions.length; i++) {
			if(!functions[i].equals(p.functions[i]))
				return false;
		}
		for(int i = 0; i < variables.length; i++) {
			if(!variables[i].equals(p.variables[i]))
				return false;
		}
		return true;
	}

	@Override
	public DeclarationModifiers getModifiers() {
		throw new IllegalStateException("Units don't have modifiers");
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
			if(var.signature.name.equals(name))
				return var;
		}
		return null;
	}
	
	public FunctionPrototype[] getFunctions(String name) {
		return ArrayOperator.filter(functions, f -> f.getName().equals(name));
	}

	@Override
	public UnitDeclaration getAccess(Unit unit) {
		if(!unit.fullBase.equals(fullBase))
			throw new IllegalArgumentException("Unit " + unit + " does not match prototype " + this);
		return unit.declaration;
	}

	/** A unit prototype equals a unit declaration iff they have the same full base. */
	@Override
	public boolean matchesDeclaration(ValueDeclaration decl) {
		return decl instanceof UnitDeclaration && ((UnitDeclaration) decl).fullBase.equals(fullBase);
	}
	
	@Override
	public Signature getSignature() {
		throw new IllegalAccessError("Unit prototypes have no signatures");
	}
	
}
