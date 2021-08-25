package fr.wonder.ahk.compiler.tokens;

import fr.wonder.ahk.compiled.units.SourceElement;
import fr.wonder.ahk.compiled.units.SourceReference;

public class Token implements SourceElement {
	
	public final TokenBase base;
	
	public final SourceReference sourceRef;
	public final String text;
	
	public Token sectionPair;
	
	public Token(SourceReference sourceRef, TokenBase base, String text) {
		this.sourceRef = sourceRef;
		this.base = base;
		this.text = text;
	}
	
	@Override
	public SourceReference getSourceReference() {
		return sourceRef;
	}
	
	public void linkSectionPair(Token pair) {
		this.sectionPair = pair;
		pair.sectionPair = this;
	}
	
	public String descriptiveString() {
		return "\""+text+"\"["+sourceRef.start+":"+sourceRef.stop+":"+base+"]";
	}
	
	@Override
	public String toString() {
		return text+"("+base+")";
	}

}
