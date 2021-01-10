package fr.wonder.ahk;

import fr.wonder.ahk.compiled.AHKManifest;

public class AHKProjectHandle {
	
	public final UnitSource[] units;
	public final AHKManifest manifest;
	
	public AHKProjectHandle(UnitSource[] units, AHKManifest manifest) {
		this.units = units;
		this.manifest = manifest;
	}
	
}
