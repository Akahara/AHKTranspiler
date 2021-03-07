package fr.wonder.ahk.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiler.linker.Linker;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public class Natives {

	public static final String ahkImportBase = "ahk.";

	private static final Map<String, Unit> nativeUnits = new HashMap<>();

	public static boolean isNativeBase(String unitBase) {
		return unitBase.startsWith(ahkImportBase);
	}
	
	/**
	 * Loads the non-loaded natives units and their dependencies recursively.
	 * Returns all required native units, already fully linked. <br>
	 * 
	 * @param importations the full base of required native units, must contain only
	 *                     native unit bases
	 * @param errors       the error wrapper used to parse all native units, should
	 *                     display the source of the import
	 * @return the list of units required to use the unit of {@code importation} or
	 *         null if {@code importation} cannot be loaded
	 * @throws WrappedException if a native unit cannot be loaded
	 */
	public static Set<Unit> getUnits(Collection<String> importations, ErrorWrapper errors) throws WrappedException {
		for(String imp : importations)
			loadUnit(imp, errors.subErrrors("Loading dependency " + imp));

		Set<Unit> units = new HashSet<>();
		List<String> toBeLoaded = new ArrayList<>(new HashSet<>(importations)); // filter duplicates
		
		// 'recursively' pass on all imported units
		for(int i = 0; i < toBeLoaded.size(); i++) {
			String imp = toBeLoaded.get(i);
			Unit u = nativeUnits.get(imp);
			if(u == null) {
				errors.add("Native unit " + imp + " could not be loaded");
			} else {
				units.add(u);
				for(String ui : u.importations) {
					if(!toBeLoaded.contains(ui))
						toBeLoaded.add(ui);
				}
			}
		}
		errors.assertNoErrors();
		return units;
	}
	
	private static Unit loadUnit(String fullBase, ErrorWrapper errors) {
		if (!isNativeBase(ahkImportBase))
			throw new IllegalArgumentException("Unit '" + fullBase + "' is not part of the standard lib");
		
		if(nativeUnits.containsKey(fullBase))
			return nativeUnits.get(fullBase);
		nativeUnits.put(fullBase, null); // to be replaced after the unit has been loaded

		// read native unit
		ErrorWrapper unitErrors = errors.subErrrors("Unable to compile unit");
		String unitRawSource = readNativeSource(fullBase, errors);
		if(unitRawSource == null)
			return null;
		// parse native unit
		UnitSource unitSource = new UnitSource(fullBase, unitRawSource);
		Unit unit;
		try {
			unit = UnitParser.parseUnit(unitSource, unitErrors);
			Linker.prelinkUnit(unit, unitErrors);
		} catch (WrappedException e) {
			return null;
		}
		nativeUnits.put(fullBase, unit);
		// load dependencies
		UnitPrototype[] importedUnits = new UnitPrototype[unit.importations.length];
		boolean loadedSuccessfully = true;
		for (int i = 0; i < unit.importations.length; i++) {
			Unit u = loadUnit(unit.importations[i], unitErrors);
			if(u != null)
				importedUnits[i] = u.prototype;
			else
				loadedSuccessfully = false;
		}
		// FUTURE when struct type is implemented, fix the native units types table
		if(loadedSuccessfully) {
			Linker.linkUnit(unit, importedUnits, new TypesTable(), unitErrors);
			return unit;
		} else {
			nativeUnits.put(fullBase, null);
			return null;
		}
	}
	
	private static String readNativeSource(String fullBase, ErrorWrapper errors) {
		String path = "/ahk/natives/" + fullBase.substring(ahkImportBase.length()).replaceAll("\\.", "/") + ".ahk";
		InputStream in = Natives.class.getResourceAsStream(path);
		if (in == null) {
			errors.add("Missing source");
			return null;
		}
		String unitRawSource;
		try {
			unitRawSource = new String(in.readAllBytes());
		} catch (IOException e) {
			throw new IllegalStateException("Unable to read unit source", e);
		}
		return unitRawSource;
	}

}
