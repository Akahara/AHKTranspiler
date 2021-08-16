package fr.wonder.ahk.handles;

import fr.wonder.ahk.compiled.AHKManifest;
import fr.wonder.ahk.compiled.units.Unit;

public class TranspilableHandle {
	
	public final Unit[] units;
	public final AHKManifest manifest;
	
	public TranspilableHandle(Unit[] units, AHKManifest manifest) {
		this.units = units;
		this.manifest = manifest;
	}
	
}
