package fr.wonder.ahk.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import fr.wonder.ahk.UnitSource;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public class Natives {

	public static final String ahkImportBase = "ahk.";

	private static final Map<String, Unit> nativeUnits = new HashMap<>();
	private static final Set<String> loadedUnits = new HashSet<>();
	
	/**
	 * Loads the native unit which full base is {@code importation} and all of its
	 * dependencies recursively, returns a list containing all units needed by
	 * {@code importation} (some may have already been loaded).
	 * <br>
	 * 
	 * @param importation the full base of the native unit
	 * @param errors      the error wrapper used to parse all native units, should
	 *                    display the source of the import
	 * @return the list of units required to use the unit of {@code importation} or
	 *         null if {@code importation} cannot be loaded
	 * @throws WrappedException if a native unit cannot be loaded
	 */
	public static Set<Unit> getUnits(Collection<String> importations, ErrorWrapper errors) throws WrappedException {
		for(String i : importations)
			loadUnit(i, errors);
		errors.assertNoErrors();

		Set<Unit> units = new HashSet<>();
		Deque<Unit> toBeLoaded = new ArrayDeque<>();
		for(String i : importations) {
			Unit unit = nativeUnits.get(i);
			if(units.add(unit))
				toBeLoaded.add(unit);
		}
		errors.assertNoErrors();
		// 'recursively' pass on all imported units
		Unit u;
		while ((u = toBeLoaded.pollFirst()) != null) {
			for(String ui : u.importations) {
				if(units.add(nativeUnits.get(ui)))
					toBeLoaded.add(nativeUnits.get(ui));
			}
		}
		return units;
	}

	private static void loadUnit(String fullBase, ErrorWrapper errors) throws WrappedException {
		if (!fullBase.startsWith(ahkImportBase))
			throw new IllegalArgumentException("Unit '" + fullBase + "' is not part of the standard lib");
		
		if(!loadedUnits.add(fullBase))
			return;

		String path = "/ahk/natives/" + fullBase.substring(ahkImportBase.length()).replaceAll("\\.", "/") + ".ahk";
		InputStream in = Natives.class.getResourceAsStream(path);
		if (in == null) {
			errors.add("Missing native unit:" + fullBase);
			errors.assertNoErrors();
		}
		try {
			ErrorWrapper unitErrors = errors.subErrrors("Unable to compile native unit " + fullBase);
			UnitSource unitSource = new UnitSource(fullBase, new String(in.readAllBytes()));
			Unit unit = UnitParser.parseUnit(unitSource, unitErrors);
			unitErrors.assertNoErrors();
//			Linker.prelinkUnit(unit, unitErrors); TODO link the native units here rather than in Linker#link
//			unitErrors.assertNoErrors();
			nativeUnits.put(fullBase, unit);
			for (String uimport : unit.importations)
				loadUnit(uimport, unitErrors);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to read native source of " + fullBase, e);
		}
	}

}
