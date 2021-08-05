package fr.wonder.ahk.compiler.parser;

import static fr.wonder.ahk.compiler.tokens.TokenBase.LIT_FLOAT;
import static fr.wonder.ahk.compiler.tokens.TokenBase.LIT_INT;
import static fr.wonder.ahk.compiler.tokens.TokenBase.LIT_STR;
import static fr.wonder.ahk.compiler.tokens.TokenBase.TK_DOT;

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
		boolean quoteIsComment = false;
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
						if(!quoteIsComment) {
							int ss = quoteBeginPosition;
							int st = i;
							tokens.add(new Token(source, LIT_STR, source.substring(ss, st), ss, st));
						}
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
				
				int ss = i;
				int st = i+latestOpenedSection.syntax.length();
				Token t = new Token(source, latestOpenedSection, source.substring(ss, st), ss, st);
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
					quoteIsComment = del == SectionToken.SEC_COMMENTS || del == SectionToken.SEC_LINE_COMMENT;
				} else {
					Token t = new Token(source, del.start, source.substring(i, stop), i, stop);
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
		String text = source.substring(begin, end);
		TokenBase b = getBase(text);
		if(b == null)
			errors.add("Unresolved token:" + source.getErr(begin, end));
		else
			tokens.add(new Token(source, b, text, begin, end));
	}
	
	private static TokenBase getBase(String split) {
		for(TokenBase b : Tokens.BASES)
			if(split.matches(b.syntax))
				return b;
		return null;
	}
	
	private void finalizeTokens() {
		// remove spaces
		tokens.removeIf(t -> t.base == TokenBase.TK_SPACE || t.base == TokenBase.TK_NL);
		
		for(int i = 1; i < tokens.size(); i++) {
			// previous, current and next tokens
			Token ptk = tokens.get(i-1);
			Token tk = tokens.get(i);
			Token ntk = i+1 < tokens.size() ? tokens.get(i+1) : null;
			
			if(tk.base == TK_DOT) {
				// replace <(int) . int> and <int . (int)> by <float>
				String floatText = "";
				int floatStart = tk.sourceStart;
				int floatStop = tk.sourceStop;
				boolean isFloat = false;
				if(ptk.base == LIT_INT) {
					floatText = ptk.text;
					floatStart = ptk.sourceStart;
					tokens.remove(i-1);
					i--;
					isFloat = true;
				}
				floatText += ".";
				if(ntk != null && ntk.base == LIT_INT) {
					floatText += ntk.text;
					floatStop = ntk.sourceStop;
					tokens.remove(i+1);
					isFloat = true;
				}
				if(isFloat)
					tokens.set(i, new Token(source, LIT_FLOAT, floatText, floatStart, floatStop));
				
				// replace <Unit . variable> by <variable>
				if( ntk != null &&
					ptk.base == TokenBase.VAR_UNIT &&
					ntk.base == TokenBase.VAR_VARIABLE) {
					
					tokens.set(i, new Token(tk.getSource(), TokenBase.VAR_VARIABLE,
							ptk.text + tk.text + ntk.text, ptk.sourceStart, ntk.sourceStop));
					tokens.remove(i+1);
					tokens.remove(i-1);
					i--;
				}
			}
		}
	}

	public static Token[] tokenize(UnitSource source, ErrorWrapper errors) {
		Tokenizer instance = new Tokenizer(source, errors);
		instance.tokenize();
		instance.finalizeTokens();
		return instance.tokens.toArray(Token[]::new);
	}
	
}
