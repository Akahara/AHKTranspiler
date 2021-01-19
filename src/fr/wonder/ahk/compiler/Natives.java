package fr.wonder.ahk.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.wonder.ahk.UnitSource;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public class Natives {
	
	public static final String ahkImportBase = "ahk.";
	
	private static final List<Unit> nativeUnits = new ArrayList<>();

	public static List<Unit> getUnits(String importation, ErrorWrapper errors) throws WrappedException {
		if(!importation.startsWith(ahkImportBase))
			throw new IllegalArgumentException("Unit '" + importation + "' is not part of the standard lib");
		
		for(Unit u : nativeUnits) {
			if(u.fullBase.equals(importation))
				return Arrays.asList(u);
		}
		
		String path = "/ahk/natives/" + importation.substring(ahkImportBase.length()).replaceAll("\\.", "/") + ".ahk";
		InputStream in = Natives.class.getResourceAsStream(path);
		if(in == null) {
			errors.add("Missing native unit:" + importation); // TODO print the unit that imported the missing one
			return null;
		}
		try {
			ErrorWrapper unitErrors = errors.subErrrors("Unable to compile native unit " + importation);
			UnitSource unitSource = new UnitSource(importation, new String(in.readAllBytes()));
			Unit unit = UnitParser.parseUnit(unitSource, unitErrors);
			if(!unitErrors.noErrors())
				return null;
			nativeUnits.add(unit);
			List<Unit> units = new ArrayList<>();
			units.add(unit);
			for(String uimport : unit.importations) {
				if(uimport.startsWith(ahkImportBase)) {
					List<Unit> loadedUnits = getUnits(uimport, unitErrors);
					if(loadedUnits != null)
						units.addAll(loadedUnits);
				} else {
					errors.add("Native unit imported non-native unit:" + uimport);
				}
			}
			return units;
		} catch (IOException e) {
			throw new IllegalStateException("Unable to read native source of " + importation, e);
		}
	}
	
}
