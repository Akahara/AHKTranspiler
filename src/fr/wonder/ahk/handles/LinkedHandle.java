package fr.wonder.ahk.handles;

import fr.wonder.ahk.compiled.AHKManifest;
import fr.wonder.ahk.compiled.units.Unit;

public class LinkedHandle {
	
	public final Unit[] units;
	public final AHKManifest manifest;
	
	public LinkedHandle(Unit[] units, AHKManifest manifest) {
		this.units = units;
		this.manifest = manifest;
	}
	
}
