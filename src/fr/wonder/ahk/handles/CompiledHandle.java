package fr.wonder.ahk.handles;

import fr.wonder.ahk.compiler.Unit;
import fr.wonder.ahk.compiler.linker.Linker;
import fr.wonder.commons.exceptions.ErrorWrapper;
import fr.wonder.commons.exceptions.ErrorWrapper.WrappedException;

public class CompiledHandle {

	public final Unit[] units;
	
	public CompiledHandle(Unit[] units) {
		this.units = units;
	}
	
	public LinkedHandle link(ErrorWrapper errors) throws WrappedException {
		for(Unit u : units)
			Linker.prelinkUnit(u, errors);
		return Linker.link(this, errors);
	}

}
