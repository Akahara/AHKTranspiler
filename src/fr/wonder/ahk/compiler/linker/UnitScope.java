package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.sections.DeclarationVisibility;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.commons.exceptions.ErrorWrapper;
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
	
	private Tuple<UnitPrototype, String> getUnitFromVarName(String name, SourceElement srcElem, ErrorWrapper errors) {
		int dot = name.indexOf('.');
		if(dot != -1) {
			String unitName = name.substring(0, dot);
			String varName = name.substring(dot+1);
			for(UnitPrototype proto : importedUnits) {
				if(proto.base.equals(unitName))
					return new Tuple<>(proto, varName);
			}
			// there is no accessible unit with the given name
			errors.add("'" + unitName + "' is not a unit" + srcElem.getErr());
			return null;
		} else {
			return new Tuple<>(this.unit, name);
		}
	}
	
	@Override
	public VarAccess getVariable(String name, SourceElement srcElem, ErrorWrapper errors) {
		Tuple<UnitPrototype, String> tuple = getUnitFromVarName(name, srcElem, errors);
		if(tuple == null)
			return Invalids.VARIABLE_PROTO;
		UnitPrototype unit = tuple.a;
		name = tuple.b;
		// note that no function and variable can have the same name
		VariablePrototype varProto = unit.getVariable(name);
		if(varProto != null && (unit.fullBase.equals(this.unit.fullBase) || varProto.modifiers.visibility == DeclarationVisibility.GLOBAL))
			return varProto;
		// and no two functions can have the same name either
		FunctionPrototype func = unit.getFunction(name);
		if(func != null && (func.signature.declaringUnit.equals(this.unit.fullBase) || func.modifiers.visibility == DeclarationVisibility.GLOBAL))
			return func;
		
		errors.add("Usage of undeclared variable " + name + srcElem.getErr());
		return Invalids.VARIABLE_PROTO;
	}

	@Override
	public void registerVariable(VarAccess var, SourceElement srcElem, ErrorWrapper errors) {
		throw new IllegalStateException("Invalid scope state");
	}
	
}