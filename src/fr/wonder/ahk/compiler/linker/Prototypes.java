package fr.wonder.ahk.compiler.linker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.prototypes.FunctionPrototype;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiled.units.prototypes.VarAccess;
import fr.wonder.ahk.compiled.units.prototypes.VariablePrototype;
import fr.wonder.ahk.compiled.units.sections.FunctionSection;
import fr.wonder.commons.utils.ArrayOperator;

public class Prototypes {

	/** Affects and returns the computed prototype of the unit */
	static void buildPrototype(Unit unit) {
		FunctionPrototype[] functions = ArrayOperator.map(
				unit.functions,
				FunctionPrototype[]::new,
				FunctionSection::getPrototype);
		VariablePrototype[] variables = ArrayOperator.map(
				unit.variables,
				VariablePrototype[]::new,
				VariableDeclaration::getPrototype);
		unit.prototype = new UnitPrototype(
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
