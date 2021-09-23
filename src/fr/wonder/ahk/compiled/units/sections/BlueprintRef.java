package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.units.prototypes.BlueprintPrototype;

public class BlueprintRef {
	
	public final String name;
	public BlueprintPrototype blueprint;
	
	public BlueprintRef(String name) {
		this.name = name;
	}
	
	@Override
	public boolean equals(Object obj) {
		return obj instanceof BlueprintRef && blueprint.matchesPrototype(((BlueprintRef) obj).blueprint);
	}
	
}
