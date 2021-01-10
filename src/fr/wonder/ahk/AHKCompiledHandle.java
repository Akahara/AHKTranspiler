package fr.wonder.ahk;

import java.util.HashMap;
import java.util.Map;

import fr.wonder.ahk.compiled.AHKManifest;
import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.compiler.types.TypesTable;

public class AHKCompiledHandle {

	public final TypesTable typesTable = new TypesTable();
	public final AHKManifest manifest;
	public final Unit[] units;
	// set by the linker while exploring importations
	public Unit[] nativeRequirements;
	// filled by the linker while exploring functions and variables
	public final Map<Unit, ValueDeclaration[]> externFields = new HashMap<>();
	
	public AHKCompiledHandle(AHKManifest manifest, Unit[] units) {
		this.manifest = manifest;
		this.units = units;
	}
	
	public boolean requiresNative(String base) {
		for(Unit u : nativeRequirements)
			if(u.getFullBase().equals(base))
				return true;
		return false;
	}

}
