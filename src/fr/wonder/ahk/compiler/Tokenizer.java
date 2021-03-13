package fr.wonder.ahk.compiler;

import static fr.wonder.ahk.compiler.tokens.TokenBase.*;

import java.util.LinkedList;
import java.util.List;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiler.tokens.SectionToken;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.compiler.tokens.TokenBase;
import fr.wonder.ahk.compiler.tokens.Tokens;
import fr.wonder.commons.exceptions.ErrorWrapper;

public class Tokenizer {
	
	private final UnitSource source;
	private final ErrorWrapper errors;
	
	private final List<Token> tokens = new LinkedList<>();
	private final LinkedList<SectionToken> openedSections = new LinkedList<>();
	private final LinkedList<Token> openedSectionTokens = new LinkedList<>();
	
	private Tokenizer(UnitSource source, ErrorWrapper errors) {
		this.source = source;
		this.errors = errors;
	}
	
	private void tokenize() {
		TokenBase quoteEnd = null;
		boolean quoteIgnored = false;
		int quoteBeginPosition = -1;
		
		TokenBase latestOpenedSection = null;
		
		int currentTokenBegin = 0;
		
		for(int i = 0; i < source.length(); ) {
			// read a text character
			if(quoteEnd != null) {
				if(source.charAt(i) == '\\') {
					if(i == source.length()-1)
						errors.add("Unexpected source end " + source.getErr(i));
					i += 2;
				} else {
					if(source.matchesRaw(quoteEnd.syntax, i)) {
						if(!quoteIgnored)
							tokens.add(new Token(source, LIT_STR, source.substring(quoteBeginPosition, i), quoteBeginPosition));
						i += quoteEnd.syntax.length();
						quoteEnd = null;
						currentTokenBegin = i;
					} else {
						// advance by 1 text character
						i++;
					}
				}
				continue;
			}
			
			// check if the section closes
			if(latestOpenedSection != null && source.matchesRaw(latestOpenedSection.syntax, i)) {
				readNonSectionToken(currentTokenBegin, i);
				
				Token t = new Token(source, latestOpenedSection, source.substring(i, i+latestOpenedSection.syntax.length()), i);
				t.linkSectionPair(openedSectionTokens.getLast());
				tokens.add(t);
				openedSections.removeLast();
				openedSectionTokens.removeLast();
				
				i += latestOpenedSection.syntax.length();
				latestOpenedSection = getLatestOpenedSection();
				currentTokenBegin = i;
				continue;
			}
			
			// check for a section begin
			SectionToken del = getMatchingSection(i);
			if(del != null) {
				readNonSectionToken(currentTokenBegin, i);
				
				int delLen = del.start.syntax.length();
				int stop = i+delLen;
				if(del.repeatable)
					while(source.matchesRaw(del.start.syntax, stop))
						stop += delLen;
				if(del.quote) {
					quoteEnd = del.stop;
					quoteBeginPosition = stop;
					quoteIgnored = del == SectionToken.SEC_COMMENTS;
				} else {
					Token t = new Token(source, del.start, source.substring(i, stop), i);
					tokens.add(t);
					if(del.stop != null) {
						openedSections.add(del);
						openedSectionTokens.add(t);
						latestOpenedSection = del.stop;
					}
				}
				
				i = stop;
				currentTokenBegin = i;
				continue;
			}
			
			// no token could be read, advance by 1
			i++;
		}
		
		// read the last non section token
		readNonSectionToken(currentTokenBegin, source.length());
		
		// check unclosed sections
		if(!openedSections.isEmpty()) {
			errors.add("Unclosed section:" + openedSectionTokens.getLast().getErr());
		} else if(quoteEnd != null) {
			errors.add("Unclosed text section:" + source.getErr(quoteBeginPosition));
		}
		
	}
	
	private TokenBase getLatestOpenedSection() {
		if(openedSections.isEmpty())
			return null;
		return openedSections.getLast().stop;
	}
	
	private SectionToken getMatchingSection(int loc) {
		for(SectionToken del : Tokens.SECTIONS) {
			if(source.matchesRaw(del.start.syntax, loc)) {
				return del;
			}
		}
		return null;
	}
	
	private void readNonSectionToken(int begin, int end) {
		if(begin == end)
			return;
		TokenBase b = getBase(source.substring(begin, end));
		if(b == null)
			errors.add("Unresolved token:" + source.getErr(begin, end));
		else
			tokens.add(new Token(source, b, source.substring(begin, end), begin));
	}
	
	private static TokenBase getBase(String split) {
		for(TokenBase b : Tokens.BASES)
			if(split.matches(b.syntax))
				return b;
		return null;
	}
	
	private void finalizeTokens() {
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
		
		// remove spaces
		for(int i = tokens.size()-1; i >= 0; i--)
			if(tokens.get(i).base == TokenBase.TK_SPACE)
				tokens.remove(i);
	}

	public static Token[] tokenize(UnitSource source, ErrorWrapper errors) {
		Tokenizer instance = new Tokenizer(source, errors);
		instance.tokenize();
		instance.finalizeTokens();
		return source.tokens = instance.tokens.toArray(Token[]::new);
	}
	
}
