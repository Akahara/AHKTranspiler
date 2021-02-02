package fr.wonder.ahk.handles;

import fr.wonder.ahk.compiled.AHKManifest;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.compiler.types.TypesTable;

public class AHKTranspilableHandle {
	
	public final Unit[] units;
	public final Unit[] nativeRequirements;
	public final TypesTable typesTable;
	public final AHKManifest manifest;
	
	public AHKTranspilableHandle(Unit[] units, Unit[] nativeRequirements,
			TypesTable typesTable, AHKManifest manifest) {
		this.units = units;
		this.nativeRequirements = nativeRequirements;
		this.typesTable = typesTable;
		this.manifest = manifest;
	}
	
	public boolean requiresNative(String base) {
		for(Unit u : nativeRequirements)
			if(u.fullBase.equals(base))
				return true;
		return false;
	}
	
}
