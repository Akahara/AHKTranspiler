package fr.wonder.ahk.compiler.linker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.Prototype;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.commons.utils.ArrayOperator;

public class Prototypes {

	// FUTURE rework the #getRecompilableUnits method
	
	public static List<UnitPrototype> getRecompilableUnits(
			List<UnitPrototype> units,
			UnitPrototype previousProto,
			UnitPrototype newProto) {
		
		if(previousProto == null || previousProto == newProto) // '==' cannot be used here !!!!!! use #matchesPrototype instead
			return Collections.emptyList();
		
		List<Prototype<?>> affectedAccesses = new ArrayList<>();
		for(FunctionPrototype func : previousProto.functions) {
			FunctionPrototype other = newProto.getFunction(func.getName());
			if(other == null || !func.matchesPrototype(other))
				affectedAccesses.add(func);
		}
		for(VariablePrototype var : previousProto.variables) {
			VariablePrototype other = newProto.getVariable(var.getName());
			if(other == null || !var.matchesPrototype(other))
				affectedAccesses.add(var);
		}
		if(affectedAccesses.isEmpty())
			return Collections.emptyList();
		List<UnitPrototype> affectedUnits = new ArrayList<>();
		for(UnitPrototype unit : units) {
			if(!ArrayOperator.contains(unit.importations, previousProto.fullBase))
				continue;
			for(Prototype<?> external : unit.externalAccesses) {
				if(affectedAccesses.contains(external)) {
					affectedUnits.add(unit);
					break;
				}
			}
		}
		return affectedUnits;
	}

}
