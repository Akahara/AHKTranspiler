package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.commons.types.Tuple;

class UnitScope implements Scope {
	
	private final UnitPrototype unit;
	private final UnitPrototype[] importedUnits;
	
	UnitScope(UnitPrototype unit, UnitPrototype[] importedUnits) {
		this.unit = unit;
		this.importedUnits = importedUnits;
	}

	@Override
	public Scope innerScope() {
		return new InnerScope(this);
	}

	@Override
	public Scope outerScope() {
		throw new IllegalStateException("Invalid scope state");
	}

	@Override
	public UnitScope getUnitScope() {
		return this;
	}
	
	private Tuple<UnitPrototype, String> getUnitFromVarName(String name) {
		int dot = name.indexOf('.');
		if(dot != -1) {
			String unitName = name.substring(0, dot);
			String varName = name.substring(dot+1);
			for(UnitPrototype proto : importedUnits) {
				if(proto.base.equals(unitName))
					return new Tuple<>(proto, varName);
			}
			// will occur if this unit scope is generated with missing unit prototypes
			// (like missing native units for example)
			throw new IllegalStateException("Unknown unit " + unitName);
		} else {
			return new Tuple<>(this.unit, name);
		}
	}
	
	@Override
	public VarAccess getVariable(String name) {
		Tuple<UnitPrototype, String> tuple = getUnitFromVarName(name);
		UnitPrototype unit = tuple.a;
		name = tuple.b;
		if(unit == null)
			return null;
		// note that no function and variable can have the same name
		VarAccess varProto = unit.getVariable(name);
		if(varProto != null)
			return varProto;
		// also the linker will do the job of searching for the right function, no need to do that here
		// it only requires a function to be found to begin its work TODO this is a bit funky
		FunctionPrototype[] functions = unit.getFunctions(name);
		if(functions.length != 0)
			return functions[0];
		return null;
	}

	@Override
	public void registerVariable(VarAccess var) {
		throw new IllegalStateException("Invalid scope state");
	}
	
	FunctionPrototype[] getFunctions(String name) {
		Tuple<UnitPrototype, String> tuple = getUnitFromVarName(name);
		UnitPrototype unit = tuple.a;
		name = tuple.b;
		if(unit == null)
			return null;
		return unit.getFunctions(name);
	}
	
}