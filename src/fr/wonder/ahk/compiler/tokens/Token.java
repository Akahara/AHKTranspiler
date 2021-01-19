package fr.wonder.ahk.compiler.tokens;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiled.units.SourceObject;

public class Token extends SourceObject {
	
	public final TokenBase base;
	
	public final String text;
	
	public Token sectionPair;
	
	public Token(UnitSource source, TokenBase base, String text, int sourceStart) {
		super(source, sourceStart, sourceStart+text.length());
		this.base = base;
		this.text = text;
	}
	
	public void linkSectionPair(Token pair) {
		this.sectionPair = pair;
		pair.sectionPair = this;
	}
	
	public String descriptiveString() {
		return "\""+text+"\"["+sourceStart+":"+sourceStop+":"+base+"]";
	}
	
	@Override
	public String toString() {
		return text+"("+base+")";
	}

}
