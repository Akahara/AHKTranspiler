package fr.wonder.ahk.compiler.linker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.Prototype;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.commons.utils.ArrayOperator;

public class Prototypes {

	public static List<UnitPrototype> getRecompilableUnits( // TODO use Prototype#matchesPrototype instead of #equals
			List<UnitPrototype> units,
			UnitPrototype previousProto,
			UnitPrototype newProto) {
		
		if(previousProto == null || previousProto == newProto)
			return Collections.emptyList();
		
		List<Prototype<?>> affectedAccesses = new ArrayList<>();
		for(FunctionPrototype func : previousProto.functions) {
			FunctionPrototype[] functions = newProto.getFunctions(func.getName());
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
			if(!Objects.equals(var, newProto.getVariable(var.getName())))
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
