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
import fr.wonder.ahk.compiled.units.prototypes.UnitPrototype;
import fr.wonder.ahk.compiler.linker.Linker;
import fr.wonder.ahk.compiler.types.TypesTable;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public class Natives {

	public static final String ahkImportBase = "ahk.";

	private static final Map<String, Unit> nativeUnits = new HashMap<>();
	private static final Set<String> loadedUnits = new HashSet<>();
	
	/**
	 * Loads the non-loaded natives units and their dependencies recursively.
	 * Returns all required native units, already pre-linked and linked.
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
		while (!toBeLoaded.isEmpty()) {
			Unit u = toBeLoaded.pollFirst();
			if(u == null) {
				errors.add("A native unit could not be loaded"); // TODO maybe keep track of unit names instead of
				errors.assertNoErrors();
			}
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
			Linker.prelinkUnit(unit, unitErrors);
			unitErrors.assertNoErrors();
			nativeUnits.put(fullBase, unit);
			for (String uimport : unit.importations)
				loadUnit(uimport, unitErrors);
			// TODO when struct type is implemented, fix the native units types table
			Linker.linkUnit(unit, getImportedUnits(unit), new TypesTable(), unitErrors);
			unitErrors.assertNoErrors();
		} catch (IOException e) {
			throw new IllegalStateException("Unable to read native source of " + fullBase, e);
		}
	}
	
	private static UnitPrototype[] getImportedUnits(Unit unit) {
		UnitPrototype[] prototypes = new UnitPrototype[unit.importations.length];
		for(int i = 0; i < prototypes.length; i++)
			prototypes[i] = nativeUnits.get(unit.importations[i]).prototype;
		return prototypes;
	}

}
