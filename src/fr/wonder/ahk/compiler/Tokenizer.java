package fr.wonder.ahk.compiler;

import static fr.wonder.ahk.compiler.tokens.TokenBase.*;

import java.util.ArrayList;
import java.util.List;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiler.tokens.SectionToken;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class Tokenizer {

	public static Token[] tokenize(UnitSource source, ErrorWrapper errors) {
		List<Token> tokens = new ArrayList<>();
		List<SectionToken> openedSections = new ArrayList<>();
		List<Token> openedSectionTokens = new ArrayList<>();
		SectionToken quoteBegin = null;
		boolean quoteIgnore = false;
		
		// find delimiters
		for(int i = 0; i < source.length(); i++) {
			if(quoteBegin != null) {
				if(quoteIgnore) {
					quoteIgnore = false;
				} else if(source.charAt(i) == '\\') {
					quoteIgnore = true;
				} else {
					TokenBase closing = quoteBegin.stop != null ? quoteBegin.stop : quoteBegin.start;
					int len = closing.syntax.length();
					if(source.matchesRaw(closing.syntax, i, i+len)) {
						int start = tokens.get(tokens.size()-1).sourceStop;
						tokens.add(new Token(source, quoteBegin == SectionToken.SEC_COMMENTS ? null : LIT_STR, source.substring(start, i), start));
						tokens.add(new Token(source, null, source.substring(i, i+len), i)); // quotes are removed later
						i += len-1;
						quoteBegin = null;
					}
				}
			} else {
				SectionToken currentSection = openedSections.size()==0 ? 
						null : openedSections.get(openedSections.size()-1);
				
				for(SectionToken del : Tokens.SECTIONS) {
					int stopSyntaxLen = del.stop==null ? 0 : del.stop.syntax.length();
					int startSyntaxLen = del.start.syntax.length();
					
					// check for section stop
					if(del == currentSection && source.matchesRaw(del.stop.syntax, i, i+stopSyntaxLen)) {
						Token t = new Token(source, del.stop, source.substring(i, i+stopSyntaxLen), i);
						t.linkSectionPair(openedSectionTokens.get(openedSectionTokens.size()-1));
						tokens.add(t);
						i += stopSyntaxLen-1;
						openedSections.remove(openedSections.size()-1);
						openedSectionTokens.remove(openedSectionTokens.size()-1);
						break;
						
					// check for section begin
					} else if(source.matchesRaw(del.start.syntax, i, i+startSyntaxLen)) {
						int stop = i+del.start.syntax.length();
						if(del.repeatable)
							while(source.matchesRaw(del.start.syntax, stop, stop+startSyntaxLen))
								stop += startSyntaxLen;
						if(del.quote) {
							quoteBegin = del;
							tokens.add(new Token(source, null, source.substring(i, stop), i));
						} else {
							Token t = new Token(source, del.start, source.substring(i, stop), i);
							tokens.add(t);
							if(del.stop != null) {
								openedSections.add(del);
								openedSectionTokens.add(t);
							}
						}
						i = stop-1;
						break;
						
					// check unexpected section stop
					} else if(del.stop != null && source.matchesRaw(del.stop.syntax, i, i+stopSyntaxLen)) {
						errors.add("Unexpected section end:" + source.getErr(i, i+stopSyntaxLen));
						break;
					}
				}
			}
		}
		
		// check unclosed sections
		if(openedSections.size() != 0) {
			errors.add("Unclosed section:" + openedSectionTokens.get(openedSectionTokens.size()-1).getErr());
		} else if(quoteBegin != null) {
			errors.add("Unclosed text section:" + tokens.get(tokens.size()-1).getErr());
		}
		
		// find non-delimiters
		int lastOpening = 0;
		int token = 0;
		while(lastOpening != source.length()) {
			if(token < tokens.size() && tokens.get(token).sourceStart == lastOpening) {
				lastOpening = tokens.get(token++).sourceStop;
			} else {
				int stop = token == tokens.size() ? source.length() : tokens.get(token).sourceStart;
				String s = source.substring(lastOpening, stop);
				TokenBase b = getBase(s);
				if(b != null) {
					tokens.add(token++, new Token(source, b, source.substring(lastOpening, lastOpening+s.length()), lastOpening));
				} else {
					errors.add("Unresolved token:" + source.getErr(lastOpening, stop));
				}
				lastOpening = stop;
			}
		}
		
		// replace <int dot int> by <float>
		for(int i = 0; i < tokens.size(); i++) {
			if(tokens.get(i).base == LIT_INT) {
				int length = 0;
				if(i+1 < tokens.size() && tokens.get(i+1).base == TK_DOT) {
					length++;
					if(i+2 < tokens.size() && tokens.get(i+2).base == LIT_INT)
						length++;
				}
				if(length > 0) {
					int start = tokens.get(i).sourceStart;
					int stop = tokens.get(i+length).sourceStop;
					tokens.set(i, new Token(source, LIT_FLOAT, source.substring(start, stop), start));
					if(length > 1)
						tokens.remove(i+2);
					tokens.remove(i+1);
				}
			}
		}
		
		// remove quotes markers and spaces
		for(int i = tokens.size()-1; i >= 0; i--)
			if(tokens.get(i).base == null || tokens.get(i).base == TokenBase.TK_SPACE)
				tokens.remove(i);
		
		return source.tokens = tokens.toArray(Token[]::new);
	}
	
	private static TokenBase getBase(String split) {
		for(TokenBase b : Tokens.BASES)
			if(split.matches(b.syntax))
				return b;
		return null;
	}
	
}
