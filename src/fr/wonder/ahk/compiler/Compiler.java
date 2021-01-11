package fr.wonder.ahk.compiler;

import fr.wonder.ahk.AHKCompiledHandle;
import fr.wonder.ahk.AHKProjectHandle;
import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.commons.exceptions.AssertionException;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class Compiler {

	/**
	 * @param unitaryComp whether the project should be compiled as a whole or as a
	 *                    collection of units.
	 */
	public static AHKCompiledHandle compile(AHKProjectHandle project, ErrorWrapper errors, boolean unitaryComp)
			throws AssertionException {
		Token[][] unitsTokens = tokenizeUnits(project, errors.subErrrors("Unable to tokenize all units"));
		errors.assertNoErrors();

		Unit[] units = parseUnits(project, unitsTokens, errors.subErrrors("Unable to parse all units"));
		errors.assertNoErrors();

		AHKCompiledHandle compiled = new AHKCompiledHandle(project.manifest, units);

		project.manifest.validate(compiled, errors, unitaryComp);
		errors.assertNoErrors();

		return compiled;
	}

	private static Token[][] tokenizeUnits(AHKProjectHandle project, ErrorWrapper errors) {
		Token[][] tokens = new Token[project.units.length][];
		for (int i = 0; i < project.units.length; i++) {
			UnitSource source = project.units[i];
			tokens[i] = Tokenizer.tokenize(source, errors.subErrrors("Token errors in unit " + source.name));
		}
		return tokens;
	}

	private static Unit[] parseUnits(AHKProjectHandle project, Token[][] unitsTokens, ErrorWrapper errors) {
		Unit[] units = new Unit[project.units.length];
		for (int i = 0; i < project.units.length; i++) {
			try {
				UnitSource source = project.units[i];
				units[i] = UnitParser.parseUnit(source, unitsTokens[i],
						errors.subErrrors("Parsing errors in unit " + source.name));
			} catch (AssertionException x) {
				// catching there only results in skipping a part of
				// the compilation of the current source
			}
		}
		return units;
	}

}
