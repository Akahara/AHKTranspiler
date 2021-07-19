package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.exceptions.ErrorWrapper;

class StructSectionParser {

	public static StructSection parseStruct(UnitSource source, Token[][] lines, int begin, int end,
			DeclarationModifiers modifiers, ErrorWrapper errors) {
		Token[] declaration = lines[begin];
		if(declaration.length != 3) {
			errors.add("Invalid struct declaration:" + source.getErr(declaration));
			return Invalids.STRUCT;
		} else if(declaration[1].base != TokenBase.VAR_UNIT) {
			errors.add("Expected struct name:" + declaration[1].getErr());
			return Invalids.STRUCT;
		}
		
		List<VariableDeclaration> members = new ArrayList<>();
		
		for(int i = begin+1; i < end; i++) {
			Token[] line = lines[i];
			
			if(Tokens.isVarType(line[0].base)) {
				members.add(StatementParser.parseVariableDeclaration(source, line, errors));
			} else {
				errors.add("Unexpected line begin token in struct declaration:" + source.getErr(line));
			}
		}
		
		String structName = declaration[1].text;
		return new StructSection(source, begin, end, structName, modifiers, null);
	}

}
