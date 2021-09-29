package fr.wonder.ahk.compiled.units.sections;

import fr.wonder.ahk.compiled.units.prototypes.blueprints.BlueprintPrototype;

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
	
	@Override
	public String toString() {
		return name;
	}
	
}
