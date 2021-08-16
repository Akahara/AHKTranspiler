package fr.wonder.ahk.handles;

import fr.wonder.ahk.compiled.AHKManifest;
import fr.wonder.ahk.compiled.units.Unit;

public class LinkedHandle {
	
	public final Unit[] units;
	
	public LinkedHandle(Unit[] units) {
		this.units = units;
	}
	
	public TranspilableHandle prepare(AHKManifest manifest) {
		return new TranspilableHandle(units, manifest);
	}
	
}
