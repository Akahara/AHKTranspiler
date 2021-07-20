package fr.wonder.ahk.handles;

import fr.wonder.ahk.compiled.AHKManifest;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiler.types.TypesTable;

public class TranspilableHandle {
	
	public final Unit[] units;
	public final TypesTable typesTable;
	public final AHKManifest manifest;
	
	public TranspilableHandle(Unit[] units, TypesTable typesTable, AHKManifest manifest) {
		this.units = units;
		this.typesTable = typesTable;
		this.manifest = manifest;
	}
	
}
