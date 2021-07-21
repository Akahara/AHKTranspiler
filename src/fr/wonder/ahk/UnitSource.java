package fr.wonder.ahk;

import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiler.tokens.Token;
import fr.wonder.ahk.utils.Utils;
import fr.wonder.commons.annotations.PseudoFinal;

public class UnitSource {
	
	public final String name;
	
	public final String rawSource;
	public final String source;
	
	private final int[] linebreaks;
	
	/** Set by the Tokenizer */
	@PseudoFinal
	public Token[] tokens;
	
	public UnitSource(String name, String rawSource) {
		this.name = name;
		rawSource = rawSource.replaceAll("\t", " ");
		this.rawSource = rawSource;
		this.source = rawSource.replaceAll("\n", " ");
		this.linebreaks = new int[Utils.countChar(rawSource, '\n')];
		int lastBreak = -1;
		for(int i = 0; i < linebreaks.length; i++)
			linebreaks[i] = lastBreak = rawSource.indexOf('\n', lastBreak+1);
	}
	
	public char charAt(int i) {
		return source.charAt(i);
	}
	
	public String substring(int start, int stop) {
		return source.substring(start, stop);
	}
	
	public int length() {
		return source.length();
	}
	
	public boolean matches(String regex, int start, int stop) {
		return length() >= stop && source.substring(start, stop).matches(regex);
	}
	
	public boolean matchesRaw(String syntax, int start) {
		return source.startsWith(syntax, start);
	}
	
	private int getLineIdx(int chrIdx) {
		for(int i = 0; i < linebreaks.length; i++) {
			if(chrIdx < linebreaks[i])
				return i;
		}
		return linebreaks.length;
	}
	
	private String getLine(int line) {
		int charL = line>0 ? linebreaks[line-1] : 0;
		int charR = line<linebreaks.length ? linebreaks[line] : source.length();
		return source.substring(charL, charR);
	}

	public String getLine(SourceElement s) {
		return getLine(getLineIdx(s.getSourceStart()));
	}
	
	public int getTokenIndexAt(int chrIdx) {
		int i = 0;
		while(i < tokens.length && tokens[i].sourceStop < chrIdx)
			i++;
		if(i == tokens.length || tokens[i].sourceStart > chrIdx)
			return -1;
		return i;
	}
	
	public Token getTokenAt(int chrIdx) {
		int idx = getTokenIndexAt(chrIdx);
		return idx == -1 ? null : tokens[idx];
	}
	
	public String getErr(int chrIdx) {
		int l = getLineIdx(chrIdx);
		String line = getLine(l);
		int llen = line.length();
		line = line.stripLeading();
		int spacing = chrIdx-(l>0?linebreaks[l-1]:0)+line.length()-llen;
		return  "\n  At " + name + " - " + (l+1) + ":" + spacing +
				"\n    " + line +
				"\n    " + " ".repeat(spacing) + "^";
	}
	
	public String getErr(int chrBegin, int chrEnd) {
		int l = getLineIdx(chrBegin);
		String line = getLine(l);
		int llen = line.length();
		line = line.stripLeading();
		int spacing = chrBegin-(l>0?linebreaks[l-1]:0)+line.length()-llen;
		return  "\n  At " + name + " - " + (l+1) + ":" + (chrBegin-(l>0?linebreaks[l-1]:0)) +
				"\n    " + line +
//				"\n    " + " ".repeat(spacing) + "^" + "~".repeat(Math.max(0, Math.min(j-i, llen-spacing)-1));
				"\n    " + " ".repeat(spacing) + "~".repeat(Math.max(0, Math.min(chrEnd-chrBegin, llen-spacing)));
	}
	
	public String getErr(SourceElement s) {
		return getErr(s.getSourceStart(), s.getSourceStop());
	}
	
	public <T extends SourceElement> String getErr(T[] t) {
		return getErr(t, 0, t.length);
	}
	
	public <T extends SourceElement> String getErr(T[] t, int start, int stop) {
		return getErr(t[start].getSourceStart(), t[stop-1].getSourceStop());
	}
	
}
