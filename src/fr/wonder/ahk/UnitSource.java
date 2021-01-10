package fr.wonder.ahk;

import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.utils.Utils;

public class UnitSource {
	
	public final String name;
	
	public final String rawSource;
	public final String source;
	
	private final int[] linebreaks;
	
	public UnitSource(String name, String rawSource) {
		this.name = name;
		rawSource = rawSource.replaceAll("\t", "  ");
		this.rawSource = rawSource;
		this.source = rawSource.replaceAll("\n", " ");
		this.linebreaks = new int[Utils.countChar(rawSource, '\n')];
		int lastBreak = 0;
		for(int i = 0; i < rawSource.length(); i++) {
			if(rawSource.charAt(i)=='\n')
				this.linebreaks[lastBreak++] = i;
		}
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
	
	public boolean matchesRaw(String syntax, int start, int stop) {
		return length() >= stop && source.substring(start, stop).equals(syntax);
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
	
	public String getErr(int i) {
		int l = getLineIdx(i);
		String line = getLine(l);
		int llen = line.length();
		line = line.stripLeading();
		int spacing = i-(l>0?linebreaks[l-1]:0)+line.length()-llen;
		return  "\n  At " + name + " - " + (l+1) + ":" + spacing +
				"\n    " + line +
				"\n    " + " ".repeat(spacing) + "^";
	}
	
	public String getErr(int i, int j) {
		int l = getLineIdx(i);
		String line = getLine(l);
		int llen = line.length();
		line = line.stripLeading();
		int spacing = i-(l>0?linebreaks[l-1]:0)+line.length()-llen;
		return  "\n  At " + name + " - " + (l+1) + ":" + (i-(l>0?linebreaks[l-1]:0)) +
				"\n    " + line +
//				"\n    " + " ".repeat(spacing) + "^" + "~".repeat(Math.max(0, Math.min(j-i, llen-spacing)-1));
				"\n    " + " ".repeat(spacing) + "~".repeat(Math.max(0, Math.min(j-i, llen-spacing)));
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
