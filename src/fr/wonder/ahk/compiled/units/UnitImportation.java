package fr.wonder.ahk.compiled.units;

import fr.wonder.ahk.compiler.Unit;

public class UnitImportation extends SourceObject {
	
	public final String unitBase;
	/** Set by the linker */
	public Unit unit;
	
	public UnitImportation(String importation, Unit unit, int sourceStart, int sourceStop) {
		super(unit, sourceStart, sourceStop);
		this.unitBase = importation;
	}
	
}
