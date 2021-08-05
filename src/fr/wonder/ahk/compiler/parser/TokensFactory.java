package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class TokensFactory {

	public static Token[][] splitTokens(Token[] tokens, ErrorWrapper errors) {
		List<Token[]> lines = new ArrayList<>();
		
		int begin = 0;
		int openedSections = 0;
		for(int i = 0; i < tokens.length; i++) {
			for(TokenBase b : Tokens.SPLITS) {
				if(tokens[i].base == b) {
					if(i > begin) {
						// keep the '{' at the end of the line
						int end = i;
						if(tokens[i].base != TokenBase.TK_LINE_BREAK)
							end++;
						// add a line composed of all encountered tokens since last 
						// section begin excluding split losses
						lines.add(Arrays.copyOfRange(tokens, begin, end));
						for(int j = begin; j < end; j++) {
							Token pair = tokens[j].sectionPair;
							if(pair != null && pair.base != TokenBase.TK_BRACE_CLOSE && pair.sourceStart > tokens[i].sourceStop) {
								errors.add("Unexpected break in section:" + tokens[i].getErr() + "\nOpened section:" + tokens[j].getErr());
							}
						}
					}
					if(tokens[i].base == TokenBase.TK_BRACE_OPEN) {
						openedSections++;
					} else if(tokens[i].base == TokenBase.TK_BRACE_CLOSE) {
						openedSections--;
						// add a line only composed of '}'
						lines.add(new Token[] { tokens[i] });
					}
					begin = i+1;
					break;
				}
			}
			if(begin == i-1 && Tokens.isDeclarationVisibility(tokens[begin].base)) {
				lines.add(new Token[] { tokens[begin] });
				begin = i;
			}
		}
		if(begin != tokens.length)
			lines.add(Arrays.copyOfRange(tokens, begin, tokens.length));
		
		if(openedSections != 0)
			throw new IllegalStateException("Unexpected unit end!" + tokens[tokens.length-1].getErr());
		return lines.toArray(Token[][]::new);
	}

	/**
	 * Used to move tokens with single line body to their own line (if, else,
	 * for...).
	 */
	public static Token[][] finalizeTokens(Token[][] tokens) {
		List<Token[]> lines = Utils.asList(tokens);
		// replace single line tokens
		for(int i = 0; i < lines.size(); i++) {
			Token[] line = lines.get(i);
			if(line.length > 1 && Utils.arrayContains(Tokens.SINGLE_LINE_KEYWORDS, line[0].base)) {
				if(line[1].base == TokenBase.TK_PARENTHESIS_OPEN) {
					int closing = Utils.getFirstIndex(line, line[1].sectionPair);
					if(closing != -1 && line.length > closing+1 && line[closing+1].base != TokenBase.TK_BRACE_OPEN) {
						lines.set(i, Arrays.copyOfRange(line, 0, closing+1));
						lines.add(i+1, Arrays.copyOfRange(line, closing+1, line.length));
					}
				} else {
					if(line[1].base != TokenBase.TK_BRACE_OPEN) {
						lines.set(i, new Token[] {line[0]});
						lines.add(i+1, Arrays.copyOfRange(line, 1, line.length));
					}
				}
			}
		}
		
//		// remove empty lines (<should> be useless) TODO0 remove commented code
//		for(int i = lines.size()-1; i >= 0; i--) {
//			if(lines.get(i).length == 0) {
//				lines.remove(i);
//			}
//		}
		
		return lines.toArray(Token[][]::new);
	}

}
