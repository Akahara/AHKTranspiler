package fr.wonder.ahk.handles;

import fr.wonder.ahk.compiled.AHKManifest;
import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.compiler.types.TypesTable;

public class AHKLinkedHandle {
	
	/**
	 * The list of units in this handle, contains both source units and all imported
	 * native units of {@link #nativeRequirements} at the end of the array.
	 */
	public final Unit[] units;
	public final Unit[] nativeRequirements;
	
	public final TypesTable typesTable = new TypesTable();
	
	public AHKLinkedHandle(Unit[] units, Unit[] nativeRequirements) {
		this.units = units;
		this.nativeRequirements = nativeRequirements;
	}
	
	public AHKTranspilableHandle prepare(AHKManifest manifest) {
		return new AHKTranspilableHandle(units, nativeRequirements, typesTable, manifest);
	}
	
}
