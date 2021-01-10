package fr.wonder.ahk.compiler;

import fr.wonder.ahk.AHKCompiledHandle;
import fr.wonder.ahk.AHKProjectHandle;
import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiler.linker.Linker;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.utils.ErrorWrapper;

public class Compiler {
	
	public static AHKCompiledHandle compile(AHKProjectHandle project, ErrorWrapper errors) {
		System.out.println("Compiling project...");
		
		System.out.println("Tokenizing units...");
		Token[][] unitsTokens = tokenizeUnits(project, errors.subErrrors("Unable to tokenize all units"));
		errors.assertNoErrors();
		System.out.println("Tokenized units.");
		
		System.out.println("Parsing units...");
		Unit[] units = parseUnits(project, unitsTokens, errors.subErrrors("Unable to parse all units"));
		errors.assertNoErrors();
		System.out.println("Parsed units.");
		
		AHKCompiledHandle compiled = new AHKCompiledHandle(project.manifest, units);
		
		project.manifest.validate(compiled, errors);
		errors.assertNoErrors();
		
		System.out.println("Linking project...");
		Linker.link(compiled, errors.subErrrors("Unable to link all units"));
		errors.assertNoErrors();
		System.out.println("Linked project.");
		
		System.out.println("Compiled project.");
		
		return compiled;
	}
	
	private static Token[][] tokenizeUnits(AHKProjectHandle project, ErrorWrapper errors) {
		Token[][] tokens = new Token[project.units.length][];
		for(int i = 0; i < project.units.length; i++) {
			UnitSource source = project.units[i];
			tokens[i] = Tokenizer.tokenize(source, errors.subErrrors("Token errors in unit " + source.name));
		}
		return tokens;
	}
	
	private static Unit[] parseUnits(AHKProjectHandle project, Token[][] unitsTokens, ErrorWrapper errors) {
		Unit[] units = new Unit[project.units.length];
		for(int i = 0; i < project.units.length; i++) {
			UnitSource source = project.units[i];
			units[i] = UnitParser.parseUnit(source, unitsTokens[i], errors.subErrrors("Parsing errors in unit " + source.name));
		}
		return units;
	}
	
}
