package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.statements.VariableDeclaration;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.StructSection;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.exceptions.ErrorWrapper;

class StructSectionParser {

	public static StructSection parseStruct(Unit unit, Token[][] lines, int begin, int end,
			DeclarationModifiers modifiers, ErrorWrapper errors) {
		Token[] declaration = lines[begin];
		if(declaration.length != 3) {
			errors.add("Invalid struct declaration:" + unit.source.getErr(declaration));
			return Invalids.STRUCT;
		} else if(declaration[1].base != TokenBase.VAR_UNIT) {
			errors.add("Expected struct name:" + declaration[1].getErr());
			return Invalids.STRUCT;
		}
		
		List<VariableDeclaration> members = new ArrayList<>();
		
		for(int i = begin+1; i < end; i++) {
			Token[] line = lines[i];
			
			if(Tokens.isVarType(line[0].base)) {
				members.add(StatementParser.parseVariableDeclaration(unit, line, errors));
			} else {
				errors.add("Unexpected line begin token in struct declaration:" + unit.source.getErr(line));
			}
		}
		
		String structName = declaration[1].text;
		return new StructSection(
				unit.source,
				declaration[0].sourceStart,
				declaration[declaration.length-1].sourceStop,
				structName,
				modifiers,
				members.toArray(VariableDeclaration[]::new));
	}

}
