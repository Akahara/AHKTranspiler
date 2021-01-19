package fr.wonder.ahk.handles;

import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.compiler.linker.Linker;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public class AHKCompiledHandle {

	public final Unit[] units;
	
	public AHKCompiledHandle(Unit[] units) {
		this.units = units;
	}
	
	public AHKLinkedHandle link(ErrorWrapper errors) throws WrappedException {
		return Linker.link(this, errors);
	}

}
