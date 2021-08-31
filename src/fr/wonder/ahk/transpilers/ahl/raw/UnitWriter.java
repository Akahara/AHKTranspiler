package fr.wonder.ahk.transpilers.ahl.raw;

import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class UnitWriter {
	
	public final Unit unit;
	public final ErrorWrapper errors;
	
	public UnitWriter(Unit unit, ErrorWrapper errors) {
		this.unit = unit;
		this.errors = errors;
	}
	
}
