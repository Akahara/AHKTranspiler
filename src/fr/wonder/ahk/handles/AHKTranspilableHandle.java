package fr.wonder.ahk.handles;

import fr.wonder.ahk.compiled.AHKManifest;
import fr.wonder.ahk.compiler.LinkedUnit;
import fr.wonder.ahk.compiler.types.TypesTable;

public class AHKTranspilableHandle {
	
	public final LinkedUnit[] units;
	public final LinkedUnit[] nativeRequirements;
	public final TypesTable typesTable;
	public final AHKManifest manifest;
	
	public AHKTranspilableHandle(LinkedUnit[] units, LinkedUnit[] nativeRequirements,
			TypesTable typesTable, AHKManifest manifest) {
		this.units = units;
		this.nativeRequirements = nativeRequirements;
		this.typesTable = typesTable;
		this.manifest = manifest;
	}
	
	public boolean requiresNative(String base) {
		for(LinkedUnit u : nativeRequirements)
			if(u.fullBase.equals(base))
				return true;
		return false;
	}
	
}
