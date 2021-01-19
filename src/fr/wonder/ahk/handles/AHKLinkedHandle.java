package fr.wonder.ahk.handles;

import java.util.HashMap;
import java.util.Map;

import fr.wonder.ahk.compiled.AHKManifest;
import fr.wonder.ahk.compiled.expressions.ValueDeclaration;
import fr.wonder.ahk.compiler.LinkedUnit;
import fr.wonder.ahk.compiler.types.TypesTable;

public class AHKLinkedHandle {
	
	/**
	 * The list of units in this handle, contains both source units and all imported
	 * native units of {@link #nativeRequirements} at the end of the array.
	 */
	public final LinkedUnit[] units;
	public final LinkedUnit[] nativeRequirements;
	
	public final TypesTable typesTable = new TypesTable();
	// filled by the linker while exploring functions and variables
	public final Map<LinkedUnit, ValueDeclaration[]> externFields = new HashMap<>();
	
	public AHKLinkedHandle(LinkedUnit[] units, LinkedUnit[] nativeRequirements) {
		this.units = units;
		this.nativeRequirements = nativeRequirements;
	}
	
	public AHKTranspilableHandle prepare(AHKManifest manifest) {
		return new AHKTranspilableHandle(units, nativeRequirements, typesTable, manifest);
	}
	
}
