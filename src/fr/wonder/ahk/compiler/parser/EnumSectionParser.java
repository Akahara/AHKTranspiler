package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.compiled.units.Unit;
import fr.wonder.ahk.compiled.units.sections.DeclarationModifiers;
import fr.wonder.ahk.compiled.units.sections.EnumSection;
import fr.wonder.ahk.compiler.Invalids;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class EnumSectionParser extends AbstractParser {

	public static EnumSection parseEnum(Unit unit, Token[][] lines, int begin, int end,
			DeclarationModifiers structModifiers, ErrorWrapper errors) {

		Token[] declaration = lines[begin];
		
		Pointer p = new Pointer(1);
		
		EnumSection enumeration;
		
		try {
			String enumName = assertToken(declaration, p, TokenBase.VAR_ENUM_NAME, "Expected enum name", errors).text;
			assertToken(declaration, p, TokenBase.TK_BRACE_OPEN, "Expected '{'", errors);
			assertNoRemainingTokens(declaration, p, errors);
			SourceReference sourceRef = SourceReference.fromLine(declaration);
			
			enumeration = new EnumSection(sourceRef, unit, enumName, structModifiers);
		} catch (ParsingException e) {
			return Invalids.ENUM;
		}
		
		List<String> enumValues = new ArrayList<>();
		
		for(int l = begin+1; l < end; l++) {
			Token[] line = lines[l];
			
			// parse values, a trailing comma is accepted
			for(int i = 0; i < line.length; i += 2) {
				if(i+1 < line.length && line[i+1].base != TokenBase.TK_COMMA)
					errors.add("Expected ',':" + line[i+1].getErr());
				if(line[i].base != TokenBase.VAR_VARIABLE)
					errors.add("Expected enum value:" + line[i].getErr());
				else
					enumValues.add(line[i].text);
			}
		}
		
		enumeration.values = enumValues.toArray(String[]::new);
		
		return enumeration;
	}
	
	
	
}
