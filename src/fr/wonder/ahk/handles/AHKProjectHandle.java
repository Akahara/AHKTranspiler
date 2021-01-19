package fr.wonder.ahk.handles;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiler.Compiler;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public class AHKProjectHandle {
	
	public final UnitSource[] units;
	
	public AHKProjectHandle(UnitSource[] units) {
		this.units = units;
	}
	
	public AHKCompiledHandle compile(ErrorWrapper errors) throws WrappedException {
		return Compiler.compile(this, errors);
	}
	
}
