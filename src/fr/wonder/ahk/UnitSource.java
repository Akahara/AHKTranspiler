package fr.wonder.ahk;

import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.SourceReference;
import fr.wonder.ahk.utils.Utils;

public class UnitSource {
	
	public final String name;
	
	public final String source;
	
	private final int[] linebreaks;
	
	public UnitSource(String name, String rawSource) {
		this.name = name;
		rawSource = rawSource.replaceAll("\t", " ");
		this.source = rawSource;
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
			if(chrIdx <= linebreaks[i])
				return i;
		}
		return linebreaks.length;
	}
	
	private String getLine(int line) {
		int charL = line>0 ? linebreaks[line-1] : 0;
		int charR = line<linebreaks.length ? linebreaks[line] : source.length();
		return source.substring(charL, charR);
	}

	public String getLine(SourceReference s) {
		return getLine(getLineIdx(s.start));
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
		int beginLineIndex = getLineIdx(chrBegin);
		int endLineIndex = getLineIdx(chrEnd);
		int firstLineStart = beginLineIndex > 0 ? linebreaks[beginLineIndex-1] : 0;
		String error = "\n  At " + name + " - " + (beginLineIndex+1) + ":" + (chrBegin-firstLineStart);
		
		for(int l = beginLineIndex; l <= endLineIndex; l++) {
			int lineBegin = l > 0 ? linebreaks[l-1] : 0;
			int lineEnd = l < linebreaks.length ? linebreaks[l] : source.length();
			String line = source.substring(lineBegin, lineEnd);
			int llen = line.length();
			line = line.stripLeading();
			int stripped = llen - line.length();
			int printBegin = Math.max(0, chrBegin - lineBegin - stripped);
			int printEnd = Math.min(line.length(), chrEnd - lineBegin - stripped);
			error += "\n    " + line +
					 "\n    " + " ".repeat(printBegin) + "~".repeat(printEnd-printBegin);
		}
		return error;
	}
	
	public String getErr(SourceReference s) {
		return getErr(s.start, s.stop);
	}
	
	public <T extends SourceElement> String getErr(T[] t) {
		return getErr(t, 0, t.length);
	}
	
	public <T extends SourceElement> String getErr(T[] t, int start, int stop) {
		return getErr(t[start].getSourceReference().start, t[stop-1].getSourceReference().stop);
	}
	
}
