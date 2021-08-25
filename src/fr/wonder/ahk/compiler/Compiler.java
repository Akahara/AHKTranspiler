package fr.wonder.ahk.compiler;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiler.optimization.UnitOptimizer;
import fr.wonder.ahk.compiler.parser.AliasDeclarationParser;
import fr.wonder.ahk.compiler.parser.Tokenizer;
import fr.wonder.ahk.compiler.parser.TokensFactory;
import fr.wonder.ahk.compiler.parser.UnitParser;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.handles.CompiledHandle;
import fr.wonder.ahk.handles.ProjectHandle;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;
import fr.wonder.commons.utils.ArrayOperator;

public class Compiler {

	public static CompiledHandle compile(ProjectHandle project, ErrorWrapper errors) throws WrappedException {
		Unit[] units = new Unit[project.units.length];
		Token[][][] unitsTokens = new Token[units.length][][];
		for (int i = 0; i < project.units.length; i++) {
			try {
				UnitSource source = project.units[i];
				ErrorWrapper subErrors = errors.subErrrors("Parsing errors in unit " + source.name);
				
				Token[] tokens = Tokenizer.tokenize(source, subErrors);
				errors.assertNoErrors();
				
				Token[][] unitTokens = TokensFactory.splitTokens(tokens, subErrors);
				unitTokens = TokensFactory.finalizeTokens(unitTokens);
				unitsTokens[i] = unitTokens;
				errors.assertNoErrors();
				
				units[i] = UnitParser.preparseUnit(source, unitTokens, subErrors);
			} catch (WrappedException x) {
				// catching there only results in skipping a part of
				// the compilation of the current source, errors will
				// still be reported in the error wrapper
			}
		}
		
		errors.assertNoErrors();
		
		assertNoDuplicates(units, errors);
		assertNoMissingImportation(units, errors);
		
		AliasDeclarationParser.resolveAliases(units, unitsTokens, errors);
		
		for(int i = 0; i < units.length; i++) {
			Unit u = units[i];
			Token[][] unitTokens = unitsTokens[i];
			
			UnitParser.parseUnit(u, unitTokens, errors);
			UnitOptimizer.optimize(project, u, errors);
		}

		return new CompiledHandle(units, project.manifest);
	}

	public static void assertNoMissingImportation(Unit[] units, ErrorWrapper errors) throws WrappedException {
		Object[] bases = ArrayOperator.map(units, u->u.fullBase);
		for(int i = 0; i < units.length; i++) {
			Unit unit = units[i];
			for(int j = 0; j < unit.importations.length; j++) {
				String importation = unit.importations[j];
				// search in the project & native units
				if(!ArrayOperator.contains(bases, importation))
					errors.add("Missing importation in unit " + unit.fullBase + " for " + importation);
			}
		}
		errors.assertNoErrors();
	}

	public static void assertNoDuplicates(Unit[] units, ErrorWrapper errors) throws WrappedException {
		// check unit duplicates
		for(int i = 0; i < units.length; i++) {
			Unit unit = units[i];
			for(int j = 0; j < i; j++) {
				if(units[j].fullBase.equals(unit.fullBase))
					errors.add("Duplicate unit found with base " + unit.fullBase);
			}
		}
		errors.assertNoErrors();
	}

}
