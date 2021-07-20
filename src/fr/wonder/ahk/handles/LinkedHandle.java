package fr.wonder.ahk.handles;

import fr.wonder.ahk.compiled.AHKManifest;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiler.types.TypesTable;

public class LinkedHandle {
	
	public final Unit[] units;
	public final TypesTable typesTable;
	
	public LinkedHandle(Unit[] units, TypesTable typesTable) {
		this.units = units;
		this.typesTable = typesTable;
	}
	
	public TranspilableHandle prepare(AHKManifest manifest) {
		return new TranspilableHandle(units, typesTable, manifest);
	}
	
}
