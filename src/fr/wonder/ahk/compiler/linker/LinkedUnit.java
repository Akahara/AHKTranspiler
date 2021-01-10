package fr.wonder.ahk.compiler.linker;

import fr.wonder.ahk.compiler.Unit;

class LinkedUnit {
	
	final UnitScope unitScope;
	final Unit unit;
	final LinkedUnit[] importations;
	
	LinkedUnit(Unit unit) {
		this.unit = unit;
		this.unitScope = new UnitScope(this);
		this.importations = new LinkedUnit[unit.importations.length];
	}

	Unit getReachableUnit(String name) {
		if(unit.name.equals(name))
			return unit;
		for(LinkedUnit u : importations)
			if(u.unit.name.equals(name))
				return u.unit;
		return null;
	}
	
	@Override
	public String toString() {
		return "lu " + unit.getFullBase();
	}
	
}