package fr.wonder.ahk.compiler.prototype;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.commons.utils.ArrayOperator;

public class Prototypes {

	/** Returns AND affects computed prototypes to every unit */
	public static UnitPrototype[] buildPrototypes(Unit[] units) {
		return ArrayOperator.map(units, UnitPrototype[]::new, Prototypes::buildPrototype);
	}
	
	/** Returns AND affects the computed prototype to unit.prototype */
	public static UnitPrototype buildPrototype(Unit unit) {
		FunctionPrototype[] functions = new FunctionPrototype[unit.functions.length];
		for(int i = 0; i < functions.length; i++) {
			FunctionSection f = unit.functions[i];
			functions[i] = new FunctionPrototype(
					unit.fullBase,
					f.name,
					f.getSignature().computedSignature,
					f.getFunctionType());
		}
		VariablePrototype[] variables = new VariablePrototype[unit.variables.length];
		for(int i = 0; i < variables.length; i++) {
			VariableDeclaration v = unit.variables[i];
			variables[i] = new VariablePrototype(
					unit.fullBase,
					v.name,
					v.getSignature().computedSignature,
					v.getType());
		}
		return unit.prototype = new UnitPrototype(
				unit.fullBase,
				unit.importations,
				functions,
				variables);
	}
	
	public static List<UnitPrototype> getRecompilableUnits(
			List<UnitPrototype> units,
			UnitPrototype previousProto,
			UnitPrototype newProto) {
		
		if(previousProto == null || previousProto == newProto)
			return Collections.emptyList();
		
		List<VarAccess> affectedAccesses = new ArrayList<>();
		for(FunctionPrototype func : previousProto.functions) {
			FunctionPrototype[] functions = newProto.getFunctions(func.name);
			if(functions.length == 0) {
				affectedAccesses.add(func);
			} else {
				boolean exists = false;
				for(FunctionPrototype fp : functions) {
					if(func.equals(fp)) {
						exists = true;
						break;
					}
				}
				if(!exists)
					affectedAccesses.add(func);
			}
		}
		for(VariablePrototype var : previousProto.variables) {
			if(!Objects.equals(var, newProto.getVariable(var.name)))
				affectedAccesses.add(var);
		}
		if(affectedAccesses.isEmpty())
			return Collections.emptyList();
		List<UnitPrototype> affectedUnits = new ArrayList<>();
		for(UnitPrototype unit : units) {
			if(!ArrayOperator.contains(unit.importations, previousProto.fullBase))
				continue;
			for(VarAccess external : unit.externalAccesses) {
				if(affectedAccesses.contains(external)) {
					affectedUnits.add(unit);
					break;
				}
			}
		}
		return affectedUnits;
	}

}
