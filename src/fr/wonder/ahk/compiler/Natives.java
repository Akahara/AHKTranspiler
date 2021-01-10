package fr.wonder.ahk.compiler;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.units.UnitImportation;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.utils.ErrorWrapper;

public class Natives {
	
	public static final String ahkImportBase = "ahk.";
	
	private static final Set<Unit> nativeUnits = new HashSet<>();

	public static List<Unit> getUnits(UnitImportation importation, ErrorWrapper errors) {
		for(Unit u : nativeUnits) {
			if(u.getFullBase().equals(importation.unitBase))
				return Arrays.asList(u);
		}
		
		String path = "/ahk/natives/" + importation.unitBase.substring(ahkImportBase.length()).replaceAll("\\.", "/") + ".ahk";
		InputStream in = Natives.class.getResourceAsStream(path);
		if(in == null) {
			errors.add("Missing native unit:" + importation.getErr());
			return null;
		}
		try {
			ErrorWrapper unitErrors = errors.subErrrors("Unable to compile native unit " + importation.unitBase);
			UnitSource unitSource = new UnitSource(importation.unitBase, new String(in.readAllBytes()));
			Token[] tokens = Tokenizer.tokenize(unitSource, unitErrors);
			if(!unitErrors.noErrors())
				return null;
			Unit unit = UnitParser.parseUnit(unitSource, tokens, unitErrors);
			if(!unitErrors.noErrors())
				return null;
			nativeUnits.add(unit);
			List<Unit> units = new ArrayList<>();
			units.add(unit);
			for(UnitImportation uimport : unit.importations) {
				if(uimport.unitBase.startsWith(ahkImportBase))
					units.addAll(getUnits(uimport, errors));
				else
					errors.add("Native unit imported non-native unit:" + uimport.getErr());
			}
			return units;
		} catch (IOException e) {
			throw new IllegalStateException("Unable to read native source of " + importation.unitBase, e);
		}
	}
	
}
