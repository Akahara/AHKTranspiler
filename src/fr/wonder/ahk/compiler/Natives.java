package fr.wonder.ahk.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import fr.wonder.ahk.UnitSource;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public class Natives {

	public static final String ahkImportBase = "ahk.";

	/**
	 * Map of loaded native units: unit full base -> list containing said unit at
	 * position 0 and all of its importations after (only the directly imported
	 * units, not the units imported by these importations...). <br>
	 * Must not be used when {@link #loadUnit(String, ErrorWrapper)} is still
	 * running as the importations lists may not be fully populated.
	 */
	private static final Map<String, List<Unit>> nativeUnits = new HashMap<>();

	/**
	 * Loads the native unit which full base is {@code importation} and all of its
	 * dependencies recursively, returns a list containing all units needed by
	 * {@code importation} (some may have already been loaded).
	 * 
	 * @param importation the full base of the native unit
	 * @param errors      the error wrapper used to parse all native units, should
	 *                    display the source of the import
	 * @return the list of units required to use the unit of {@code importation} or
	 *         null if {@code importation} cannot be loaded
	 * @throws WrappedException if a unit cannot be loaded
	 */
	public static List<Unit> getUnits(String importation, ErrorWrapper errors) throws WrappedException {
		loadUnit(importation, errors);
		errors.assertNoErrors();

		List<Unit> required = nativeUnits.get(importation);
		// set of loaded units full bases, required to avoid
		// ~infinite passes due to loading errors
		Set<String> loaded = new HashSet<>();
		// check #importation loading error
		if (required == null) {
			errors.add("Unable to load unit " + importation);
			return null;
		}
		// filter first pass
		Unit original = required.get(0);
		int m = 0;
		for (int i = 1; i < required.size(); ) {
			loaded.add(original.importations[m]);
			if(required.get(i) == null) {
				errors.add("Unable to load imported unit " + original.importations[m]);
				required.remove(i);
			} else {
				i++;
			}
			m++;
		}
		// 'recursively' pass on all imported units
		for (int i = 1; i < required.size(); i++) {
			Unit u = required.get(i);
			List<Unit> imported = nativeUnits.get(u.fullBase);
			if (imported == null) {
				errors.add("Unable to load unit " + u.fullBase);
			} else {
				for (int j = 0; j < u.importations.length; j++) {
					if(loaded.add(u.importations[j])) {
						Unit uu = imported.get(j);
						if(uu == null) {
							errors.add("Unable to load imported unit " + u.importations[j]);
						} else {
							required.add(uu);
						}
					}
				}
			}
		}

		return required;
	}

	private static Unit loadUnit(String fullBase, ErrorWrapper errors) throws WrappedException {
		if (!fullBase.startsWith(ahkImportBase))
			throw new IllegalArgumentException("Unit '" + fullBase + "' is not part of the standard lib");

		List<Unit> loaded = nativeUnits.get(fullBase);
		if (loaded != null)
			return loaded.get(0);

		String path = "/ahk/natives/" + fullBase.substring(ahkImportBase.length()).replaceAll("\\.", "/") + ".ahk";
		InputStream in = Natives.class.getResourceAsStream(path);
		if (in == null) {
			errors.add("Missing native unit:" + fullBase);
			return null;
		}
		try {
			ErrorWrapper unitErrors = errors.subErrrors("Unable to compile native unit " + fullBase);
			UnitSource unitSource = new UnitSource(fullBase, new String(in.readAllBytes()));
			Unit unit;
			try {
				unit = UnitParser.parseUnit(unitSource, unitErrors);
			} catch (WrappedException e) {
				nativeUnits.put(fullBase, null);
				return null;
			}
			if (!unitErrors.noErrors())
				return null;
			// list of directly imported units
			List<Unit> nativeImported = new ArrayList<>();
			nativeImported.add(unit);
			nativeUnits.put(fullBase, nativeImported);
			for (String uimport : unit.importations) {
				if (uimport.startsWith(ahkImportBase)) {
					nativeImported.add(loadUnit(uimport, unitErrors));
				} else {
					errors.add("Native unit imported non-native unit: " + uimport);
				}
			}
			return unit;
		} catch (IOException e) {
			throw new IllegalStateException("Unable to read native source of " + fullBase, e);
		}
	}

}
