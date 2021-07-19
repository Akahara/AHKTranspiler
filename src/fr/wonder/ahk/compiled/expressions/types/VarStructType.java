package fr.wonder.ahk.compiled.expressions.types;

import fr.wonder.ahk.compiled.units.sections.StructSection;

public class VarStructType extends VarType {
	
	public final String name;
	
	public StructSection structure;
	
	public VarStructType(String name) {
		this.name = name;
	}
	
	@Override
	public String getName() {
		return name;
	}
	
	@Override
	public String getSignature() {
		return String.format("S%02d", Math.floorMod(name.hashCode(), 100)); // FUTURE rework the struct signature
	}
	
	@Override
	public boolean equals(Object other) {
		return other instanceof VarStructType &&
				((VarStructType) other).structure.equals(structure);
	}
	
}