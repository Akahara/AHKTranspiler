package fr.wonder.ahk.handles;

import fr.wonder.ahk.UnitSource;
import fr.wonder.ahk.compiler.Compiler;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public class ProjectHandle {
	
	public final UnitSource[] units;
	
	public ProjectHandle(UnitSource[] units) {
		this.units = units;
	}
	
	public CompiledHandle compile(ErrorWrapper errors) throws WrappedException {
		return Compiler.compile(this, errors);
	}
	
}
