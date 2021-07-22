package fr.wonder.ahk.compiler.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.ahk.utils.Utils;

public class TokensFactory {

	public static Token[][] splitTokens(Token[] tokens) {
		List<Token[]> lines = new ArrayList<>();
		
		int begin = 0;
		int openedSections = 0;
		for(int i = 0; i < tokens.length; i++) {
			for(TokenBase b : Tokens.SPLITS) {
				if(tokens[i].base == b) {
					if(i > begin) {
						// keep the '{' at the end of the line
						int end = i;
//						if(openedSections != 0 && tokens[i].base != TokenBase.TK_LINE_BREAK)
						if(tokens[i].base != TokenBase.TK_LINE_BREAK)
							end++;
						// add a line composed of all encountered tokens since last 
						// section begin excluding split losses
						lines.add(Arrays.copyOfRange(tokens, begin, end));
					}
					if(tokens[i].base == TokenBase.TK_BRACE_OPEN) {
						openedSections++;
					} else if(tokens[i].base == TokenBase.TK_BRACE_CLOSE) {
						openedSections--;
						// add a line only composed of '}'
//						if(openedSections != 0)
							lines.add(new Token[] { tokens[i] });
					}
					begin = i+1;
					break;
				}
			}
			if(begin == i) {
				for(TokenBase b : Tokens.SPLIT_LOSSES) {
					if(tokens[i].base == b) {
						begin++;
						break;
					}
				}
			}
		}
		if(begin != tokens.length)
			lines.add(Arrays.copyOfRange(tokens, begin, tokens.length));
		
		if(openedSections != 0)
			throw new IllegalStateException("Unexpected unit end!" + tokens[tokens.length-1].getErr());
		return lines.toArray(Token[][]::new);
	}

	// FIX use the tokensFactory to make sure that opening and closing parenthesis are on the same line!
	/**
	 * Used to :
	 * <ul>
	 * <li>move elses and ifs with single line body to their own line</li>
	 * <li>replace token patterns 'Unit . varL' by 'varL'</li>
	 * <li>replace token patterns '- intL' by 'intL' (negative)</li>
	 * </ul>
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
		for(int i = 0; i < lines.size(); i++) {
			List<Token> line = Utils.asList(lines.get(i));
			boolean modified = false;
			for(int j = 0; j < line.size(); j++) {
				if(	j < line.size()-2 &&
					line.get(j).base == TokenBase.VAR_UNIT &&
					line.get(j+1).base == TokenBase.TK_DOT &&
					line.get(j+2).base == TokenBase.VAR_VARIABLE) {
					
					// replace 'Unit . variable' by 'variable'
					line.set(j, new Token(line.get(j).getSource(), TokenBase.VAR_VARIABLE,
							line.get(j).text+line.get(j+1).text+line.get(j+2).text, line.get(j).sourceStart));
					line.remove(j+2);
					line.remove(j+1);
					modified = true;
					
				}
//				else if(j < line.size()-1 &&
//					line.get(j).base == TokenBase.OP_MINUS && (
//					line.get(j+1).base == TokenBase.LIT_INT ||
//					line.get(j+1).base == TokenBase.LIT_FLOAT)) {
//					
//					// replace '- intL' by 'intL' or '- floatL' by 'floatL'
//					line.set(j, new Token(line.get(j).getSource(), line.get(j+1).base,
//							line.get(j).text+line.get(j+1).text, line.get(j).sourceStart));
//					line.remove(j+1);
//					modified = true;
//				}
			}
			if(modified)
				lines.set(i, line.toArray(Token[]::new));
		}
		
		// remove empty lines (<should> be useless)
		for(int i = lines.size()-1; i >= 0; i--) {
			if(lines.get(i).length == 0) {
				lines.remove(i);
			}
		}
		
		return lines.toArray(Token[][]::new);
	}

}
